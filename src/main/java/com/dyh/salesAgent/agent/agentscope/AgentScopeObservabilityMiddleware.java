package com.dyh.salesAgent.agent.agentscope;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.middleware.ReasoningInput;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/** 使用 AgentScope 2.0 Middleware 采集低基数 Micrometer 指标和请求级执行摘要。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentScopeObservabilityMiddleware implements MiddlewareBase {

    private final MeterRegistry meterRegistry;

    @Override
    public int order() {
        return 600;
    }

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext context,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        AgentScopeExecutionTracker tracker = tracker(context);
        boolean rootAgent = tracker.isRootAgent(agent.getName());
        if (!rootAgent) {
            tracker.specialistStarted(agent.getName());
        }

        Flux<AgentEvent> observed = timed(rootAgent ? "agent" : "specialist", next.apply(input));
        if (!rootAgent) {
            return observed;
        }
        return observed
                .doOnComplete(() -> tracker.complete("success"))
                .doOnError(error -> tracker.complete("error"))
                .doFinally(signal -> {
                    if (signal == SignalType.CANCEL) {
                        tracker.complete("cancelled");
                    }
                    AgentScopeExecutionSummary summary = tracker.snapshot();
                    log.info(
                            "AgentScope 调用完成 | requestId={} | rootAgent={} | status={} | durationMs={} | specialists={} | reasoning={} | modelCalls={} | toolCalls={} | inputTokens={} | outputTokens={}",
                            summary.requestId(), summary.rootAgent(), summary.status(),
                            summary.durationMs(), summary.specialistCalls(),
                            summary.reasoningRounds(), summary.modelCalls(), summary.toolCalls(),
                            summary.inputTokens(), summary.outputTokens());
                });
    }

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext context,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {
        tracker(context).reasoningStarted();
        meterRegistry.counter("agentscope.reasoning.calls").increment();
        return timed("reasoning", next.apply(input));
    }

    @Override
    public Flux<AgentEvent> onModelCall(
            Agent agent,
            RuntimeContext context,
            ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> next) {
        AgentScopeExecutionTracker tracker = tracker(context);
        tracker.modelCallStarted();
        meterRegistry.counter("agentscope.model.calls").increment();
        return timed("model", next.apply(input))
                .doOnNext(event -> {
                    if (event instanceof ModelCallEndEvent modelEnd) {
                        tracker.recordUsage(modelEnd.getUsage());
                        recordTokens(modelEnd.getUsage());
                    }
                });
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext context,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        var toolNames = input.toolCalls().stream().map(ToolUseBlock::getName).toList();
        tracker(context).toolsStarted(toolNames);
        meterRegistry.counter("agentscope.tool.calls").increment(toolNames.size());
        return timed("tool", next.apply(input));
    }

    private Flux<AgentEvent> timed(
            String stage,
            Flux<AgentEvent> publisher) {
        return Flux.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);
            AtomicReference<String> status = new AtomicReference<>("success");
            return publisher
                    .doOnError(error -> status.set("error"))
                    .doOnCancel(() -> status.set("cancelled"))
                    .doFinally(signal -> {
                        String finalStatus = signal == SignalType.CANCEL ? "cancelled" : status.get();
                        sample.stop(Timer.builder("agentscope.stage.duration")
                                .description("AgentScope lifecycle stage duration")
                                .tag("stage", stage)
                                .tag("status", finalStatus)
                                .register(meterRegistry));
                        meterRegistry.counter(
                                "agentscope.stage.completions",
                                "stage", stage,
                                "status", finalStatus).increment();
                    });
        });
    }

    private void recordTokens(io.agentscope.core.model.ChatUsage usage) {
        if (usage == null) {
            return;
        }
        meterRegistry.counter("agentscope.llm.tokens.input").increment(usage.getInputTokens());
        meterRegistry.counter("agentscope.llm.tokens.output").increment(usage.getOutputTokens());
        meterRegistry.counter("agentscope.llm.tokens.cached").increment(usage.getCachedTokens());
    }

    private AgentScopeExecutionTracker tracker(RuntimeContext context) {
        AgentScopeExecutionTracker tracker = context.get(AgentScopeExecutionTracker.class);
        if (tracker == null) {
            throw new IllegalStateException("AgentScopeExecutionTracker 未写入 RuntimeContext");
        }
        return tracker;
    }
}
