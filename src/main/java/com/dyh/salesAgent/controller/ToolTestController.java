package com.dyh.salesAgent.controller;
import com.dyh.salesAgent.tool.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 直接验证销售工具的参数校验、数据权限、Redis 缓存和查询结果。 */
@RestController
@RequestMapping("/test/tool")
@RequiredArgsConstructor
public class ToolTestController {

    private final SalesQueryTool salesQueryTool;
    private final SalesSummaryTool salesSummaryTool;
    private final SalesTrendTool salesTrendTool;
    private final ChartGeneratorTool chartGeneratorTool;
    private final AnomalyDetectionTool anomalyDetectionTool;

    // 订单查询
    record QueryRequest(String startDate, String endDate,
                        String regionName, String reqName, int limit) {}

    @PostMapping("/query-orders")
    public String queryOrders(@RequestBody QueryRequest req) {
        return salesQueryTool.queryOrders(
                req.startDate(), req.endDate(), req.regionName(), req.reqName(), req.limit());
    }

    // 汇总与排名
    record RankRequest(String startDate, String endDate, String regionName, int topN) {}
    record RangeRequest(String startDate, String endDate) {}
    record ProductRankRequest(String startDate, String endDate, int topN) {}

    @PostMapping("/top-reps")
    public String topReps(@RequestBody RankRequest req) {
        return salesSummaryTool.getTopReps(
                req.startDate(), req.endDate(), req.regionName(), req.topN());
    }

    @PostMapping("/region-ranking")
    public String regionRanking(@RequestBody RangeRequest req) {
        return salesSummaryTool.getRegionRanking(req.startDate(), req.endDate());
    }

    @PostMapping("/top-products")
    public String topProducts(@RequestBody ProductRankRequest req) {
        return salesSummaryTool.getTopProducts(req.startDate(), req.endDate(), req.topN());
    }

    // 趋势分析
    record MomRequest(String currentStart, String currentEnd,
                      String prevStart, String prevEnd, String regionName) {}
    record YoyRequest(String startDate, String endDate, String regionName) {}
    record TrendRequest(int months, String regionName) {}

    @PostMapping("/month-over-month")
    public String monthOverMonth(@RequestBody MomRequest req) {
        return salesTrendTool.calcMonthOverMonth(
                req.currentStart(), req.currentEnd(),
                req.prevStart(), req.prevEnd(), req.regionName());
    }

    @PostMapping("/year-over-year")
    public String yearOverYear(@RequestBody YoyRequest req) {
        return salesTrendTool.calcYearOverYear(
                req.startDate(), req.endDate(), req.regionName());
    }

    @PostMapping("/monthly-trend")
    public String monthlyTrend(@RequestBody TrendRequest req) {
        return salesTrendTool.getMonthlyTrend(req.months(), req.regionName());
    }

    // 图表生成
    record LineChartRequest(int months, String regionName, String title) {}
    record BarChartRequest(String dimension, String startDate, String endDate, String title) {}
    record PieChartRequest(String dimension, String startDate, String endDate, String title) {}

    @PostMapping("/line-chart")
    public String lineChart(@RequestBody LineChartRequest req) {
        return chartGeneratorTool.generateLineChart(req.months(), req.regionName(), req.title());
    }

    @PostMapping("/bar-chart")
    public String barChart(@RequestBody BarChartRequest req) {
        return chartGeneratorTool.generateBarChart(
                req.dimension(), req.startDate(), req.endDate(), req.title());
    }

    @PostMapping("/pie-chart")
    public String pieChart(@RequestBody PieChartRequest req) {
        return chartGeneratorTool.generatePieChart(
                req.dimension(), req.startDate(), req.endDate(), req.title());
    }

    // 异常检测
    @PostMapping("/detect-anomalies")
    public String detectAnomalies() {
        return anomalyDetectionTool.detectAllAnomalies();
    }
}
