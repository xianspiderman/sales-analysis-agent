package com.dyh.salesAgent.agent.agentscope;

import io.agentscope.core.model.ChatUsage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** 保存在 RuntimeContext 中的请求级统计对象，每次 Agent 调用独立创建。 */
final class AgentScopeExecutionTracker {

    private final String requestId;
    private final String rootAgentName;
    private final long startedAtNanos = System.nanoTime();
    private final AtomicInteger reasoningRounds = new AtomicInteger();
    private final AtomicInteger specialistCalls = new AtomicInteger();
    private final AtomicInteger modelCalls = new AtomicInteger();
    private final AtomicInteger toolCalls = new AtomicInteger();
    private final AtomicInteger inputTokens = new AtomicInteger();
    private final AtomicInteger outputTokens = new AtomicInteger();
    private final AtomicInteger cachedTokens = new AtomicInteger();
    private final Set<String> toolNames = new LinkedHashSet<>();
    private final Set<String> specialistNames = new LinkedHashSet<>();
    private volatile String status = "running";
    private volatile long finishedAtNanos;

    AgentScopeExecutionTracker(String requestId, String rootAgentName) {
        this.requestId = requestId;
        this.rootAgentName = rootAgentName;
    }

    boolean isRootAgent(String agentName) {
        return rootAgentName.equals(agentName);
    }

    synchronized void specialistStarted(String agentName) {
        specialistCalls.incrementAndGet();
        specialistNames.add(agentName);
    }

    void reasoningStarted() {
        reasoningRounds.incrementAndGet();
    }

    void modelCallStarted() {
        modelCalls.incrementAndGet();
    }

    void recordUsage(ChatUsage usage) {
        if (usage == null) {
            return;
        }
        inputTokens.addAndGet(usage.getInputTokens());
        outputTokens.addAndGet(usage.getOutputTokens());
        cachedTokens.addAndGet(usage.getCachedTokens());
    }

    synchronized void toolsStarted(Iterable<String> names) {
        for (String name : names) {
            toolCalls.incrementAndGet();
            toolNames.add(name);
        }
    }

    synchronized void complete(String finalStatus) {
        if (finishedAtNanos == 0L) {
            status = finalStatus;
            finishedAtNanos = System.nanoTime();
        }
    }

    synchronized AgentScopeExecutionSummary snapshot() {
        long end = finishedAtNanos == 0L ? System.nanoTime() : finishedAtNanos;
        return new AgentScopeExecutionSummary(
                requestId,
                rootAgentName,
                status,
                (end - startedAtNanos) / 1_000_000,
                specialistCalls.get(),
                List.copyOf(specialistNames),
                reasoningRounds.get(),
                modelCalls.get(),
                toolCalls.get(),
                List.copyOf(toolNames),
                inputTokens.get(),
                outputTokens.get(),
                cachedTokens.get());
    }
}
