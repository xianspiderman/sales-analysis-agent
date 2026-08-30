package com.dyh.salesAgent.agent.routing;

import com.dyh.salesAgent.agent.agentscope.AgentScopeExecutionSummary;

/** 统一入口的内部响应模型。 */
public record AgentRouteResult(
        String reply,
        AgentRouteInfo route,
        AgentScopeExecutionSummary execution
) {
}
