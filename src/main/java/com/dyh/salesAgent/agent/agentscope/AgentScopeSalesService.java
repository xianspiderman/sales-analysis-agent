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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** AgentScope ReActAgent 的应用服务，负责建立用户隔离的 RuntimeContext。 */
@Service
@Slf4j
public class AgentScopeSalesService {

    private final ReActAgent agentScopeSalesAgent;
    private final ReActAgent agentScopeSalesTeamAgent;

    public AgentScopeSalesService(
            @Qualifier("agentScopeSalesAgent") ReActAgent agentScopeSalesAgent,
            @Qualifier("agentScopeSalesTeamAgent") ReActAgent agentScopeSalesTeamAgent) {
        this.agentScopeSalesAgent = agentScopeSalesAgent;
        this.agentScopeSalesTeamAgent = agentScopeSalesTeamAgent;
    }

    public Mono<AgentScopeChatResult> chat(String sessionId, String message) {
        return chat(agentScopeSalesAgent, sessionId, message);
    }

    public Mono<AgentScopeChatResult> teamChat(String sessionId, String message) {
        return chat(agentScopeSalesTeamAgent, sessionId, message);
    }

    private Mono<AgentScopeChatResult> chat(
            ReActAgent agent,
            String sessionId,
            String message) {
        Invocation invocation = invocation(sessionId, agent.getName());
        return agent
                .call(List.of(new UserMessage(withCurrentDate(message))), invocation.runtimeContext())
                .switchIfEmpty(Mono.error(new IllegalStateException("AgentScope 未返回响应")))
                .map(result -> new AgentScopeChatResult(
                        result.getTextContent() == null ? "" : result.getTextContent(),
                        invocation.tracker().snapshot()));
    }

    public Flux<AgentScopeStreamEvent> stream(String sessionId, String message) {
        return recoverStream(streamRaw(sessionId, message), agentScopeSalesAgent, sessionId);
    }

    public Flux<AgentScopeStreamEvent> teamStream(String sessionId, String message) {
        return recoverStream(teamStreamRaw(sessionId, message), agentScopeSalesTeamAgent, sessionId);
    }

    public Flux<AgentScopeStreamEvent> streamRaw(String sessionId, String message) {
        return streamRaw(agentScopeSalesAgent, sessionId, message);
    }

    public Flux<AgentScopeStreamEvent> teamStreamRaw(String sessionId, String message) {
        return streamRaw(agentScopeSalesTeamAgent, sessionId, message);
    }

    private Flux<AgentScopeStreamEvent> streamRaw(
            ReActAgent agent,
            String sessionId,
            String message) {
        Invocation invocation = invocation(sessionId, agent.getName());
        return agent
                .streamEvents(new UserMessage(withCurrentDate(message)), invocation.runtimeContext())
                .concatMap(agentEvent -> mapEvents(agentEvent, invocation.tracker()));
    }

    private Flux<AgentScopeStreamEvent> recoverStream(
            Flux<AgentScopeStreamEvent> source,
            ReActAgent agent,
            String sessionId) {
        return source
                .onErrorResume(error -> {
                    log.error(
                            "AgentScope 流式调用失败: agent={}, sessionId={}",
                            agent.getName(), sessionId, error);
                    return Flux.just(new AgentScopeStreamEvent("error", "服务暂时不可用，请稍后重试"));
                });
    }

    public Mono<Void> clearSession(String sessionId) {
        return clearSession(agentScopeSalesAgent, sessionId);
    }

    public Mono<Void> clearTeamSession(String sessionId) {
        return clearSession(agentScopeSalesTeamAgent, sessionId);
    }

    private Mono<Void> clearSession(ReActAgent agent, String sessionId) {
        Invocation invocation = invocation(sessionId, agent.getName());
        return Mono.fromRunnable(() -> agent.clearContext(
                        invocation.runtimeContext().getUserId(),
                        invocation.runtimeContext().getSessionId()))
                .then();
    }

    private Invocation invocation(String sessionId, String rootAgentName) {
        UserContext.UserInfo user = UserContext.requireCurrent();
        // 统一执行客户端 sessionId 的格式和长度校验；AgentScope 分别保存 userId 与 sessionId。
        UserScopedMemoryId.from(user, sessionId);

        SalesAgentRuntimeContext salesContext =
                new SalesAgentRuntimeContext(user, DataScope.from(user));
        String requestId = UUID.randomUUID().toString();
        AgentScopeExecutionTracker tracker =
                new AgentScopeExecutionTracker(requestId, rootAgentName);
        String stateSessionId = AgentScopeSalesTeamConfig.TEAM_AGENT_NAME.equals(rootAgentName)
                ? "team:" + sessionId
                : sessionId;
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .userId(user.userId().toString())
                .sessionId(stateSessionId)
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
