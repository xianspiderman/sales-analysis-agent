package com.dyh.salesAgent.agent.agentscope;

import com.dyh.salesAgent.security.UserContext;
import com.dyh.salesAgent.tool.AnomalyDetectionTool;
import com.dyh.salesAgent.tool.ChartGeneratorTool;
import com.dyh.salesAgent.tool.SalesQueryTool;
import com.dyh.salesAgent.tool.SalesSummaryTool;
import com.dyh.salesAgent.tool.SalesTrendTool;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AgentScope Toolkit 到共享销售工具实现的适配层。
 * 三种执行模式复用参数校验、DataScope、缓存、查询链路和结果格式。
 */
@Component
@RequiredArgsConstructor
public class AgentScopeSalesTools {

    private final SalesQueryTool salesQueryTool;
    private final SalesSummaryTool salesSummaryTool;
    private final SalesTrendTool salesTrendTool;
    private final ChartGeneratorTool chartGeneratorTool;
    private final AnomalyDetectionTool anomalyDetectionTool;

    @Tool(name = "query_sales_orders", description = "查询原始销售订单。适用于查看某时段订单列表、订单明细或订单数量，不用于排名和趋势分析。", readOnly = true)
    public String queryOrders(
            @ToolParam(name = "startDate", description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate", description = "结束日期，格式 yyyy-MM-dd") String endDate,
            @ToolParam(name = "regionName", description = "大区名称；不限定大区时可省略", required = false) String regionName,
            @ToolParam(name = "repName", description = "销售员姓名；不限定销售员时可省略", required = false) String repName,
            @ToolParam(name = "limit", description = "最多返回条数，默认 20，最大 50") int limit,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> salesQueryTool.queryOrders(startDate, endDate, regionName, repName, limit));
    }

    @Tool(name = "get_top_sales_reps", description = "计算销售员业绩排名，可按大区筛选。", readOnly = true)
    public String getTopReps(
            @ToolParam(name = "startDate", description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate", description = "结束日期，格式 yyyy-MM-dd") String endDate,
            @ToolParam(name = "regionName", description = "大区名称；查询当前权限全部范围时可省略", required = false) String regionName,
            @ToolParam(name = "topN", description = "返回前 N 名，默认 5，最大 20") int topN,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> salesSummaryTool.getTopReps(startDate, endDate, regionName, topN));
    }

    @Tool(name = "get_region_ranking", description = "计算当前用户有权查看的大区销售业绩排名。", readOnly = true)
    public String getRegionRanking(
            @ToolParam(name = "startDate", description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate", description = "结束日期，格式 yyyy-MM-dd") String endDate,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> salesSummaryTool.getRegionRanking(startDate, endDate));
    }

    @Tool(name = "get_product_ranking", description = "计算产品销售排名。正数查询畅销产品，负数查询滞销产品。", readOnly = true)
    public String getTopProducts(
            @ToolParam(name = "startDate", description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate", description = "结束日期，格式 yyyy-MM-dd") String endDate,
            @ToolParam(name = "topN", description = "排名数量，默认 10，最大 20；负数表示查询最差的 N 名") int topN,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> salesSummaryTool.getTopProducts(startDate, endDate, topN));
    }

    @Tool(name = "get_sales_summary", description = "查询指定时段内的总销售额、订单数等销售汇总数据。", readOnly = true)
    public String getSalesSummary(
            @ToolParam(name = "startDate", description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate", description = "结束日期，格式 yyyy-MM-dd") String endDate,
            @ToolParam(name = "regionName", description = "大区名称；查询当前权限全部范围时可省略", required = false) String regionName,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> salesSummaryTool.getSalesSummary(startDate, endDate, regionName));
    }

    @Tool(name = "calculate_month_over_month", description = "计算销售环比增长率，将当前周期与上一个周期对比。", readOnly = true)
    public String calculateMonthOverMonth(
            @ToolParam(name = "currentStart", description = "当前周期开始日期，格式 yyyy-MM-dd") String currentStart,
            @ToolParam(name = "currentEnd", description = "当前周期结束日期，格式 yyyy-MM-dd") String currentEnd,
            @ToolParam(name = "previousStart", description = "对比周期开始日期；省略时自动计算", required = false) String previousStart,
            @ToolParam(name = "previousEnd", description = "对比周期结束日期；省略时自动计算", required = false) String previousEnd,
            @ToolParam(name = "regionName", description = "大区名称；查询当前权限全部范围时可省略", required = false) String regionName,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> salesTrendTool.calcMonthOverMonth(currentStart, currentEnd, previousStart, previousEnd, regionName));
    }

    @Tool(name = "calculate_year_over_year", description = "计算指定时段销售额与去年同期相比的同比增长率。", readOnly = true)
    public String calculateYearOverYear(
            @ToolParam(name = "startDate", description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate", description = "结束日期，格式 yyyy-MM-dd") String endDate,
            @ToolParam(name = "regionName", description = "大区名称；查询当前权限全部范围时可省略", required = false) String regionName,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> salesTrendTool.calcYearOverYear(startDate, endDate, regionName));
    }

    @Tool(name = "get_monthly_sales_trend", description = "查询近 N 个月的月度销售趋势，最大 24 个月。", readOnly = true)
    public String getMonthlyTrend(
            @ToolParam(name = "months", description = "查看最近多少个月，最大 24") int months,
            @ToolParam(name = "regionName", description = "大区名称；查询当前权限全部范围时可省略", required = false) String regionName,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> salesTrendTool.getMonthlyTrend(months, regionName));
    }

    @Tool(name = "generate_sales_line_chart", description = "生成销售趋势折线图的 ECharts JSON。", readOnly = true)
    public String generateLineChart(
            @ToolParam(name = "months", description = "最近多少个月的数据") int months,
            @ToolParam(name = "regionName", description = "大区名称；查询当前权限全部范围时可省略", required = false) String regionName,
            @ToolParam(name = "title", description = "图表标题") String title,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> chartGeneratorTool.generateLineChart(months, regionName, title));
    }

    @Tool(name = "generate_sales_bar_chart", description = "生成大区或销售员销售额对比柱状图的 ECharts JSON。", readOnly = true)
    public String generateBarChart(
            @ToolParam(name = "dimension", description = "对比维度：region 或 rep") String dimension,
            @ToolParam(name = "startDate", description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate", description = "结束日期，格式 yyyy-MM-dd") String endDate,
            @ToolParam(name = "title", description = "图表标题") String title,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> chartGeneratorTool.generateBarChart(dimension, startDate, endDate, title));
    }

    @Tool(name = "generate_sales_pie_chart", description = "生成大区或产品品类销售占比饼图的 ECharts JSON。", readOnly = true)
    public String generatePieChart(
            @ToolParam(name = "dimension", description = "饼图维度：region 或 category") String dimension,
            @ToolParam(name = "startDate", description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate", description = "结束日期，格式 yyyy-MM-dd") String endDate,
            @ToolParam(name = "title", description = "图表标题") String title,
            SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(),
                () -> chartGeneratorTool.generatePieChart(dimension, startDate, endDate, title));
    }

    @Tool(name = "detect_sales_anomalies", description = "扫描当前用户可见范围内的销售异常，包括订单骤降、产品零销售、退单率和业绩骤降。", readOnly = true)
    public String detectSalesAnomalies(SalesAgentRuntimeContext context) {
        return UserContext.callWith(context.user(), anomalyDetectionTool::detectAllAnomalies);
    }
}
