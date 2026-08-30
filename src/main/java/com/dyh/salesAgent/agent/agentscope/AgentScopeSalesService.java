package com.dyh.salesAgent.agent.agentscope;

import com.dyh.salesAgent.memory.UserScopedMemoryId;
import com.dyh.salesAgent.security.DataScope;
import com.dyh.salesAgent.security.UserContext;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEndEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.event.ModelCallStartEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** AgentScope ReActAgent 的应用服务，负责建立用户隔离的 RuntimeContext。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentScopeSalesService {

    private final ReActAgent agentScopeSalesAgent;

    public Mono<AgentScopeChatResult> chat(String sessionId, String message) {
        Invocation invocation = invocation(sessionId);
        return agentScopeSalesAgent
                .call(List.of(new UserMessage(withCurrentDate(message))), invocation.runtimeContext())
                .switchIfEmpty(Mono.error(new IllegalStateException("AgentScope 未返回响应")))
                .map(result -> new AgentScopeChatResult(
                        result.getTextContent() == null ? "" : result.getTextContent(),
                        invocation.tracker().snapshot()));
    }

    public Flux<AgentScopeStreamEvent> stream(String sessionId, String message) {
        Invocation invocation = invocation(sessionId);
        return agentScopeSalesAgent
                .streamEvents(new UserMessage(withCurrentDate(message)), invocation.runtimeContext())
                .concatMap(agentEvent -> mapEvents(agentEvent, invocation.tracker()))
                .onErrorResume(error -> {
                    log.error("AgentScope 流式调用失败: sessionId={}", sessionId, error);
                    return Flux.just(new AgentScopeStreamEvent("error", "服务暂时不可用，请稍后重试"));
                });
    }

    public Mono<Void> clearSession(String sessionId) {
        Invocation invocation = invocation(sessionId);
        return Mono.fromRunnable(() -> agentScopeSalesAgent.clearContext(
                        invocation.runtimeContext().getUserId(),
                        invocation.runtimeContext().getSessionId()))
                .then();
    }

    private Invocation invocation(String sessionId) {
        UserContext.UserInfo user = UserContext.requireCurrent();
        // 复用旧会话 ID 规则完成空值和长度校验，但 AgentScope 中仍分别存储 userId 与 sessionId。
        UserScopedMemoryId.from(user, sessionId);

        SalesAgentRuntimeContext salesContext =
                new SalesAgentRuntimeContext(user, DataScope.from(user));
        String requestId = UUID.randomUUID().toString();
        AgentScopeExecutionTracker tracker = new AgentScopeExecutionTracker(requestId);
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .userId(user.userId().toString())
                .sessionId(sessionId)
                .put(SalesAgentRuntimeContext.class, salesContext)
                .put(AgentScopeExecutionTracker.class, tracker)
                .put("request_id", requestId)
                .build();
        return new Invocation(runtimeContext, tracker);
    }

    private String withCurrentDate(String message) {
        return "【当前日期】" + LocalDate.now() + System.lineSeparator()
                + "【用户问题】" + message;
    }

    private AgentScopeStreamEvent mapEvent(AgentEvent event) {
        if (event instanceof AgentStartEvent start) {
            return new AgentScopeStreamEvent("agent_start", start.getReplyId());
        }
        if (event instanceof TextBlockDeltaEvent text) {
            return new AgentScopeStreamEvent("token", text.getDelta());
        }
        if (event instanceof ModelCallStartEvent modelStart) {
            return new AgentScopeStreamEvent("model_start", modelStart.getReplyId());
        }
        if (event instanceof ModelCallEndEvent modelEnd) {
            return new AgentScopeStreamEvent("model_end", modelEnd.getUsage());
        }
        if (event instanceof ToolCallStartEvent toolStart) {
            return new AgentScopeStreamEvent("tool_start", toolStart.getToolCallName());
        }
        if (event instanceof ToolResultEndEvent toolEnd) {
            return new AgentScopeStreamEvent(
                    "tool_end", toolEnd.getToolCallName() + ":" + toolEnd.getState());
        }
        return null;
    }

    private Flux<AgentScopeStreamEvent> mapEvents(
            AgentEvent event,
            AgentScopeExecutionTracker tracker) {
        if (event instanceof AgentEndEvent) {
            tracker.complete("success");
            return Flux.just(
                    new AgentScopeStreamEvent("summary", tracker.snapshot()),
                    new AgentScopeStreamEvent("done", "[DONE]"));
        }
        return Mono.justOrEmpty(mapEvent(event)).flux();
    }

    private record Invocation(
            RuntimeContext runtimeContext,
            AgentScopeExecutionTracker tracker) {
    }
}
