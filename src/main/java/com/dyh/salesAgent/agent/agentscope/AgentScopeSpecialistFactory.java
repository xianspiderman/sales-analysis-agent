package com.dyh.salesAgent.agent.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** 按最小工具权限创建一次性销售专家 Agent。 */
@Component
public class AgentScopeSpecialistFactory {

    private static final Set<String> ANALYSIS_TOOLS = Set.of(
            "query_sales_orders",
            "get_top_sales_reps",
            "get_region_ranking",
            "get_product_ranking",
            "get_sales_summary",
            "calculate_month_over_month",
            "calculate_year_over_year",
            "get_monthly_sales_trend");

    private static final Set<String> CHART_TOOLS = Set.of(
            "generate_sales_line_chart",
            "generate_sales_bar_chart",
            "generate_sales_pie_chart");

    private static final Set<String> ANOMALY_TOOLS = Set.of("detect_sales_anomalies");

    private static final String ANALYSIS_PROMPT = """
            你是销售数据分析专家，只负责订单查询、销售汇总、排名以及同比环比和趋势分析。
            必须调用工具获取事实，不得编造数据；工具结果已按当前登录用户完成数据权限过滤。
            使用中文给出清晰的数据结论，金额格式化为 ¥X,XXX，并指出最重要的一到两个变化。
            不生成图表，不执行异常扫描，不预测未来，也不修改任何数据。
            """;

    private static final String CHART_PROMPT = """
            你是销售可视化专家，只负责根据用户意图选择折线图、柱状图或饼图工具。
            必须通过图表工具生成 ECharts 数据，不得手工编造图表数据。
            工具返回 CHART_JSON: 时原样输出完整内容，不要使用 Markdown 代码块，不要改写或截断 JSON。
            不承担普通数据问答、异常诊断或未来预测。
            """;

    private static final String ANOMALY_PROMPT = """
            你是销售异常诊断专家，只负责扫描当前用户可见范围内的销售异常。
            必须调用异常检测工具，不得凭常识虚构异常；按严重程度概括发现并给出只读分析建议。
            不生成图表，不修改数据，不发送通知，也不预测未来。
            """;

    private final Model model;
    private final AgentScopeSalesTools salesTools;
    private final AgentScopeObservabilityMiddleware observabilityMiddleware;
    private final int maxIters;

    public AgentScopeSpecialistFactory(
            Model agentScopeDashScopeModel,
            AgentScopeSalesTools salesTools,
            AgentScopeObservabilityMiddleware observabilityMiddleware,
            @Value("${sales-agent.agentscope.team.specialist-max-iters:6}") int maxIters) {
        this.model = agentScopeDashScopeModel;
        this.salesTools = salesTools;
        this.observabilityMiddleware = observabilityMiddleware;
        this.maxIters = maxIters;
    }

    public ReActAgent createAnalysisAgent() {
        return createAgent(
                "sales-data-analyst",
                "负责销售订单、汇总、排名、同比环比和趋势分析",
                ANALYSIS_PROMPT,
                ANALYSIS_TOOLS);
    }

    public ReActAgent createChartAgent() {
        return createAgent(
                "sales-chart-specialist",
                "负责生成销售折线图、柱状图和饼图的 ECharts JSON",
                CHART_PROMPT,
                CHART_TOOLS);
    }

    public ReActAgent createAnomalyAgent() {
        return createAgent(
                "sales-anomaly-specialist",
                "负责扫描并解释当前权限范围内的销售异常",
                ANOMALY_PROMPT,
                ANOMALY_TOOLS);
    }

    private ReActAgent createAgent(
            String name,
            String description,
            String prompt,
            Set<String> allowedTools) {
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder()
                .parallel(true)
                .allowToolDeletion(true)
                .build());
        toolkit.registerTool(salesTools);
        List.copyOf(toolkit.getToolNames()).stream()
                .filter(toolName -> !allowedTools.contains(toolName))
                .forEach(toolkit::removeTool);

        if (!toolkit.getToolNames().equals(allowedTools)) {
            throw new IllegalStateException(
                    "专家 Agent 工具集合不符合预期: expected=" + allowedTools
                            + ", actual=" + toolkit.getToolNames());
        }

        return ReActAgent.builder()
                .name(name)
                .description(description)
                .sysPrompt(prompt)
                .model(model)
                .toolkit(toolkit)
                .middleware(observabilityMiddleware)
                .maxIters(maxIters)
                .build();
    }
}
