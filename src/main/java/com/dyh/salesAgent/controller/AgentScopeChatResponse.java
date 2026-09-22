package com.dyh.salesAgent.controller;

import com.dyh.salesAgent.agent.agentscope.AgentScopeExecutionSummary;

/** AgentScope 响应，包含一次调用的结构化执行摘要。 */
public record AgentScopeChatResponse(
        String sessionId,
        String reply,
        AgentScopeExecutionSummary execution
) {
}
