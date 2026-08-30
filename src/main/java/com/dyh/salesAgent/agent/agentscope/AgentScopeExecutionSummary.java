package com.dyh.salesAgent.agent.agentscope;

import java.util.List;

/** AgentScope 单次调用的安全执行摘要，不包含问题正文、工具参数或工具结果。 */
public record AgentScopeExecutionSummary(
        String requestId,
        String rootAgent,
        String status,
        long durationMs,
        int specialistCalls,
        List<String> specialistNames,
        int reasoningRounds,
        int modelCalls,
        int toolCalls,
        List<String> toolNames,
        int inputTokens,
        int outputTokens,
        int cachedTokens
) {
}
