package com.dyh.salesAgent.agent.agentscope;

/** 应用服务返回给 Web 层的 AgentScope 问答结果。 */
public record AgentScopeChatResult(
        String reply,
        AgentScopeExecutionSummary execution
) {
}
