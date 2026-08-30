package com.dyh.salesAgent.agent.agentscope;

/** AgentScope 事件流到项目 SSE 协议的轻量映射。 */
public record AgentScopeStreamEvent(String event, Object data) {
}
