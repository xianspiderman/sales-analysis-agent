package com.dyh.salesAgent.agent.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Supervisor + 三类销售专家的 AgentScope 多 Agent 实验配置。 */
@Configuration
public class AgentScopeSalesTeamConfig {

    static final String TEAM_AGENT_NAME = "sales-team-supervisor";

    private static final String SUPERVISOR_PROMPT = """
            你是销售分析团队的总控 Agent。你自己没有销售查询工具，必须把任务委派给合适的专家：

            - 数据查询、汇总、排名、同比环比、趋势分析：调用 delegate_sales_analysis。
            - 折线图、柱状图、饼图：调用 delegate_sales_chart。
            - 销售异常扫描和诊断：调用 delegate_sales_anomaly。

            一个问题包含多个目标时可以委派多个专家；彼此独立的任务优先并行委派。
            委派时把用户目标、日期范围、筛选条件和输出要求完整写入 message，不要要求专家超出职责。
            委派是一次性子任务，通常只传 message，不需要自行构造 session_id。
            综合回答只能使用专家返回的事实，不得补造数据。使用中文，保持简洁；金额格式化为 ¥X,XXX。
            专家返回 CHART_JSON: 时必须原样保留完整内容，不要使用 Markdown 代码块。
            只能分析和查询，不修改数据、不预测未来、不发送外部通知。
            """;

    @Bean(name = "agentScopeSalesTeamAgent", destroyMethod = "close")
    public ReActAgent agentScopeSalesTeamAgent(
            Model agentScopeDashScopeModel,
            AgentStateStore agentScopeStateStore,
            AgentScopeSpecialistFactory specialistFactory,
            AgentScopeObservabilityMiddleware observabilityMiddleware,
            @Value("${sales-agent.agentscope.team.supervisor-max-iters:8}") int maxIters) {
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder().parallel(true).build());
        registerSpecialist(
                toolkit,
                specialistFactory::createAnalysisAgent,
                "delegate_sales_analysis",
                "委派销售数据专家完成订单、汇总、排名、同比环比或趋势分析。message 必须包含完整任务条件。");
        registerSpecialist(
                toolkit,
                specialistFactory::createChartAgent,
                "delegate_sales_chart",
                "委派可视化专家生成折线图、柱状图或饼图的 ECharts JSON。message 必须包含图表类型、范围和标题要求。");
        registerSpecialist(
                toolkit,
                specialistFactory::createAnomalyAgent,
                "delegate_sales_anomaly",
                "委派异常诊断专家扫描当前用户权限范围内的销售异常并解释结果。");

        return ReActAgent.builder()
                .name(TEAM_AGENT_NAME)
                .description("根据销售问题调度数据分析、可视化和异常诊断专家")
                .sysPrompt(SUPERVISOR_PROMPT)
                .model(agentScopeDashScopeModel)
                .toolkit(toolkit)
                .middleware(observabilityMiddleware)
                .stateStore(agentScopeStateStore)
                .maxIters(maxIters)
                .build();
    }

    private void registerSpecialist(
            Toolkit toolkit,
            io.agentscope.core.tool.subagent.SubAgentProvider<ReActAgent> provider,
            String toolName,
            String description) {
        toolkit.registration()
                .subAgent(provider, SubAgentConfig.builder()
                        .toolName(toolName)
                        .description(description)
                        .forwardEvents(false)
                        .build())
                .apply();
    }
}
