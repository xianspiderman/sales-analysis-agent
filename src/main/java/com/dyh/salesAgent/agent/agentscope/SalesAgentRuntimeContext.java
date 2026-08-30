package com.dyh.salesAgent.agent.agentscope;

import com.dyh.salesAgent.security.DataScope;
import com.dyh.salesAgent.security.UserContext;

/**
 * AgentScope 单次调用的销售业务上下文。
 * 该对象通过 RuntimeContext 按类型注入工具，不进入模型可见的工具参数 Schema。
 */
public record SalesAgentRuntimeContext(
        UserContext.UserInfo user,
        DataScope dataScope
) {
    public SalesAgentRuntimeContext {
        if (user == null || dataScope == null) {
            throw new IllegalArgumentException("销售 Agent 运行上下文不能为空");
        }
    }
}
