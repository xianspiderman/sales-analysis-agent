package com.dyh.salesAgent.agent.routing;

import com.dyh.salesAgent.agent.SalesAgent;
import com.dyh.salesAgent.agent.agentscope.AgentScopeChatResult;
import com.dyh.salesAgent.agent.agentscope.AgentScopeSalesService;
import com.dyh.salesAgent.agent.agentscope.AgentScopeStreamEvent;
import com.dyh.salesAgent.memory.MysqlChatMemoryStore;
import com.dyh.salesAgent.memory.UserScopedMemoryId;
import com.dyh.salesAgent.security.UserContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** 在三条 Agent 链路之间进行配置化路由，并提供未提交响应前的安全降级。 */
@Service
@Slf4j
public class SalesAgentRoutingService {

    private final SalesAgent legacySalesAgent;
    private final AgentScopeSalesService agentScopeSalesService;
    private final MysqlChatMemoryStore chatMemoryStore;
    private final AgentRoutingProperties properties;
    private final MeterRegistry meterRegistry;

    public SalesAgentRoutingService(
            SalesAgent legacySalesAgent,
            AgentScopeSalesService agentScopeSalesService,
            MysqlChatMemoryStore chatMemoryStore,
            AgentRoutingProperties properties,
            MeterRegistry meterRegistry) {
        this.legacySalesAgent = legacySalesAgent;
        this.agentScopeSalesService = agentScopeSalesService;
        this.chatMemoryStore = chatMemoryStore;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public Mono<AgentRouteResult> chat(
            String sessionId,
            String message,
            AgentRouteMode requestedOverride) {
        UserContext.UserInfo user = UserContext.requireCurrent();
        AgentRouteMode requested = resolveMode(requestedOverride);

        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            AtomicReference<String> outcome = new AtomicReference<>("error");
            return executeChat(requested, user, sessionId, message)
                    .map(result -> routed(result, requested, requested, false))
                    .onErrorResume(primaryError -> fallbackChat(
                            requested, user, sessionId, message, primaryError))
                    .doOnNext(result -> {
                        outcome.set(result.route().fallback() ? "fallback" : "success");
                        recordRoute("chat", result.route(), outcome.get());
                    })
                    .doOnError(error -> recordFailure("chat", requested))
                    .doFinally(signal -> sample.stop(Timer.builder("sales.agent.routing.duration")
                            .description("Unified sales agent routing duration")
                            .tag("operation", "chat")
                            .tag("requested", requested.name())
                            .tag("outcome", outcome.get())
                            .register(meterRegistry)));
        });
    }

    public Flux<AgentScopeStreamEvent> stream(
            String sessionId,
            String message,
            AgentRouteMode requestedOverride) {
        UserContext.UserInfo user = UserContext.requireCurrent();
        AgentRouteMode requested = resolveMode(requestedOverride);
        AtomicBoolean committed = new AtomicBoolean();
        AtomicBoolean fallbackUsed = new AtomicBoolean();
        AtomicBoolean failed = new AtomicBoolean();
        AtomicReference<AgentRouteMode> actualMode = new AtomicReference<>(requested);

        Flux<AgentScopeStreamEvent> primary = executeStream(requested, user, sessionId, message)
                .doOnNext(event -> {
                    if ("token".equals(event.event()) || "tool_start".equals(event.event())) {
                        committed.set(true);
                    }
                });

        Flux<AgentScopeStreamEvent> routed = Flux.concat(
                        Flux.just(routeEvent("route", requested, requested, false)),
                        primary)
                .onErrorResume(primaryError -> {
                    AgentRouteMode fallback = fallbackMode(requested);
                    if (fallback == null || committed.get()) {
                        failed.set(true);
                        log.error(
                                "统一流式入口失败且不执行降级: requested={}, committed={}",
                                requested, committed.get(), primaryError);
                        return Flux.just(new AgentScopeStreamEvent(
                                "error", "服务暂时不可用，请稍后重试"));
                    }
                    fallbackUsed.set(true);
                    actualMode.set(fallback);
                    log.warn(
                            "统一流式入口触发降级: requested={}, fallback={}",
                            requested, fallback, primaryError);
                    meterRegistry.counter(
                            "sales.agent.routing.fallbacks",
                            "operation", "stream",
                            "from", requested.name(),
                            "to", fallback.name()).increment();
                    return Flux.concat(
                                    Flux.just(routeEvent("fallback", requested, fallback, true)),
                                    executeStream(fallback, user, sessionId, message))
                            .onErrorResume(fallbackError -> {
                                failed.set(true);
                                fallbackError.addSuppressed(primaryError);
                                log.error("统一流式入口降级后仍失败: fallback={}", fallback, fallbackError);
                                return Flux.just(new AgentScopeStreamEvent(
                                        "error", "服务暂时不可用，请稍后重试"));
                            });
                });

        return Flux.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            AtomicReference<String> outcome = new AtomicReference<>("error");
            return routed
                    .doOnComplete(() -> outcome.set(failed.get()
                            ? "error"
                            : fallbackUsed.get() ? "fallback" : "success"))
                    .doFinally(signal -> {
                        if (signal == reactor.core.publisher.SignalType.CANCEL) {
                            outcome.set("cancelled");
                        }
                        meterRegistry.counter(
                                "sales.agent.routing.requests",
                                "operation", "stream",
                                "requested", requested.name(),
                                "actual", actualMode.get().name(),
                                "outcome", outcome.get()).increment();
                        sample.stop(Timer.builder("sales.agent.routing.duration")
                                .description("Unified sales agent routing duration")
                                .tag("operation", "stream")
                                .tag("requested", requested.name())
                                .tag("outcome", outcome.get())
                                .register(meterRegistry));
                    });
        });
    }

    public Mono<Void> clearSession(String sessionId, AgentRouteMode requestedOverride) {
        UserContext.UserInfo user = UserContext.requireCurrent();
        AgentRouteMode mode = resolveMode(requestedOverride);
        return switch (mode) {
            case LANGCHAIN4J -> Mono.fromRunnable(() -> UserContext.runWith(
                            user,
                            () -> chatMemoryStore.deleteMessages(
                                    UserScopedMemoryId.from(user, sessionId))))
                    .subscribeOn(Schedulers.boundedElastic())
                    .then();
            case AGENTSCOPE_SINGLE -> UserContext.callWith(
                    user, () -> agentScopeSalesService.clearSession(sessionId));
            case AGENTSCOPE_TEAM -> UserContext.callWith(
                    user, () -> agentScopeSalesService.clearTeamSession(sessionId));
        };
    }

    private Mono<AgentScopeChatResult> executeChat(
            AgentRouteMode mode,
            UserContext.UserInfo user,
            String sessionId,
            String message) {
        return switch (mode) {
            case LANGCHAIN4J -> Mono.fromCallable(() -> UserContext.callWith(user, () -> {
                        String memoryId = UserScopedMemoryId.from(user, sessionId);
                        String reply = legacySalesAgent.chat(
                                memoryId, message, LocalDate.now().toString());
                        return new AgentScopeChatResult(reply, null);
                    }))
                    .subscribeOn(Schedulers.boundedElastic());
            case AGENTSCOPE_SINGLE -> UserContext.callWith(
                    user, () -> agentScopeSalesService.chat(sessionId, message));
            case AGENTSCOPE_TEAM -> UserContext.callWith(
                    user, () -> agentScopeSalesService.teamChat(sessionId, message));
        };
    }

    private Flux<AgentScopeStreamEvent> executeStream(
            AgentRouteMode mode,
            UserContext.UserInfo user,
            String sessionId,
            String message) {
        return switch (mode) {
            case LANGCHAIN4J -> legacyStream(user, sessionId, message);
            case AGENTSCOPE_SINGLE -> UserContext.callWith(
                    user, () -> agentScopeSalesService.streamRaw(sessionId, message));
            case AGENTSCOPE_TEAM -> UserContext.callWith(
                    user, () -> agentScopeSalesService.teamStreamRaw(sessionId, message));
        };
    }

    private Flux<AgentScopeStreamEvent> legacyStream(
            UserContext.UserInfo user,
            String sessionId,
            String message) {
        String memoryId = UserScopedMemoryId.from(user, sessionId);
        return Flux.create(sink -> UserContext.runWith(user, () -> legacySalesAgent
                        .chatStream(memoryId, message, LocalDate.now().toString())
                        .onPartialResponse(token -> sink.next(
                                new AgentScopeStreamEvent("token", token)))
                        .beforeToolExecution(tool -> sink.next(new AgentScopeStreamEvent(
                                "tool_start", tool.request().name())))
                        .onToolExecuted(tool -> sink.next(new AgentScopeStreamEvent(
                                "tool_end",
                                tool.request().name()
                                        + ":" + (tool.hasFailed() ? "ERROR" : "SUCCESS"))))
                        .onCompleteResponse(response -> {
                            sink.next(new AgentScopeStreamEvent("done", "[DONE]"));
                            sink.complete();
                        })
                        .onError(sink::error)
                        .start()),
                FluxSink.OverflowStrategy.BUFFER);
    }

    private Mono<AgentRouteResult> fallbackChat(
            AgentRouteMode requested,
            UserContext.UserInfo user,
            String sessionId,
            String message,
            Throwable primaryError) {
        AgentRouteMode fallback = fallbackMode(requested);
        if (fallback == null) {
            return Mono.error(primaryError);
        }
        log.warn(
                "统一同步入口触发降级: requested={}, fallback={}",
                requested, fallback, primaryError);
        meterRegistry.counter(
                "sales.agent.routing.fallbacks",
                "operation", "chat",
                "from", requested.name(),
                "to", fallback.name()).increment();
        return executeChat(fallback, user, sessionId, message)
                .map(result -> routed(result, requested, fallback, true))
                .doOnError(fallbackError -> fallbackError.addSuppressed(primaryError));
    }

    private AgentRouteResult routed(
            AgentScopeChatResult result,
            AgentRouteMode requested,
            AgentRouteMode actual,
            boolean fallback) {
        return new AgentRouteResult(
                result.reply(),
                new AgentRouteInfo(requested, actual, fallback),
                result.execution());
    }

    private AgentScopeStreamEvent routeEvent(
            String event,
            AgentRouteMode requested,
            AgentRouteMode actual,
            boolean fallback) {
        return new AgentScopeStreamEvent(
                event, new AgentRouteInfo(requested, actual, fallback));
    }

    private AgentRouteMode resolveMode(AgentRouteMode requestedOverride) {
        if (requestedOverride != null && properties.isAllowRequestOverride()) {
            return requestedOverride;
        }
        return properties.getDefaultMode();
    }

    private AgentRouteMode fallbackMode(AgentRouteMode requested) {
        if (!properties.isFallbackEnabled()
                || properties.getFallbackMode() == requested) {
            return null;
        }
        return properties.getFallbackMode();
    }

    private void recordRoute(String operation, AgentRouteInfo route, String outcome) {
        meterRegistry.counter(
                "sales.agent.routing.requests",
                "operation", operation,
                "requested", route.requestedMode().name(),
                "actual", route.actualMode().name(),
                "outcome", outcome).increment();
    }

    private void recordFailure(String operation, AgentRouteMode requested) {
        meterRegistry.counter(
                "sales.agent.routing.requests",
                "operation", operation,
                "requested", requested.name(),
                "actual", "NONE",
                "outcome", "error").increment();
    }
}
