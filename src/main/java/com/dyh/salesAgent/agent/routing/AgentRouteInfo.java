package com.dyh.salesAgent.agent.routing;

/** 一次统一入口调用最终采用的路由信息。 */
public record AgentRouteInfo(
        AgentRouteMode requestedMode,
        AgentRouteMode actualMode,
        boolean fallback
) {
}
