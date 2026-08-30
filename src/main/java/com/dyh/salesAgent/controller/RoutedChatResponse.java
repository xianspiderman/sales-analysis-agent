package com.dyh.salesAgent.controller;

import com.dyh.salesAgent.agent.agentscope.AgentScopeExecutionSummary;
import com.dyh.salesAgent.agent.routing.AgentRouteInfo;

/** 统一入口响应；LangChain4j 模式下 execution 为 null。 */
public record RoutedChatResponse(
        String sessionId,
        String reply,
        AgentRouteInfo route,
        AgentScopeExecutionSummary execution
) {
}
