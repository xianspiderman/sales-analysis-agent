package com.dyh.salesAgent.tool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dyh.salesAgent.dto.MonthlyTrendDTO;
import com.dyh.salesAgent.dto.ProductSalesDTO;
import com.dyh.salesAgent.dto.RegionSalesDTO;
import com.dyh.salesAgent.security.DataScope;
import com.dyh.salesAgent.security.ToolInputValidator;
import com.dyh.salesAgent.security.UserContext;
import com.dyh.salesAgent.service.SalesQueryService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 通过共享查询服务生成 ECharts option JSON。 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChartGeneratorTool {

    private final SalesQueryService queryService;
    private final ToolInputValidator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();//生成json数据

    @Tool("生成销售趋势折线图的 ECharts JSON 数据。适用于：画折线图、趋势图、" +
         "月度变化图等可视化需求。返回的 JSON 可直接用于前端 ECharts 渲染。")
    public String generateLineChart(
            @P("近多少个月的数据，如 6 表示近 6 个月") int months,
            @P("大区名称，如：华东区。传 null 表示全公司") String regionName,
            @P("图表标题，如：华东区近6个月销售趋势") String title) {

        log.info("工具调用-generateLineChart: months={}, region={}", months, regionName);

        try {
            int validMonths = validator.validateMonths(months);
            String validRegion = validator.validateRegionName(regionName);
            String validTitle = validator.validateTitle(title);
            Long regionId = validRegion == null ? null : queryService.getRegionIdByName(validRegion);
            if (validRegion != null && regionId == null) return "未找到大区：" + validRegion;
            DataScope scope = UserContext.requireDataScope();

            List<MonthlyTrendDTO> data = queryService.queryMonthlyTrend(scope, regionId, validMonths);
            if (data.isEmpty()) {
                return "暂无数据，无法生成图表";
            }

            List<String> xAxis = data.stream().map(MonthlyTrendDTO::month).toList();// 图的x轴
            List<Number> amounts = data.stream()
                    .map(d -> d.totalAmount().longValue())
                    .map(v -> (Number) v)
                    .toList();// 图的y轴

            Map<String, Object> option = new LinkedHashMap<>(); // 拼ECharts的option
            option.put("title", Map.of("text", validTitle != null ? validTitle : "销售趋势"));
            option.put("tooltip", Map.of("trigger", "axis"));// 图的示例展示，固定用法
            option.put("xAxis", Map.of("type", "category", "data", xAxis));// 图的x轴
            option.put("yAxis", Map.of("type", "value", "name", "销售额（元）"));// 图的y轴
            option.put("series", List.of(Map.of(
                    "type", "line",// 类型是折线图
                    "data", amounts,// y轴的数据
                    "smooth", true,// 平滑的折线图
                    "name", "销售额",
                    "itemStyle", Map.of("color", "#5470c6")// 颜色
            )));// 图的y轴的数据，放到series里面

            String json = objectMapper.writeValueAsString(option);// objectMapper转json
            return "CHART_JSON:" + json;   // 前端识别 CHART_JSON: 前缀后提取 JSON 渲染

        } catch (IllegalArgumentException e) {
            return "参数无效：" + e.getMessage();
        } catch (Exception e) {
            log.error("生成折线图失败", e);
            return "生成图表数据时出现问题，请稍后重试";
        }
    }

    @Tool("生成大区或销售员销售额对比的柱状图 ECharts JSON。适用于：画柱状图、" +
         "对比图、排行榜图等可视化需求。")
    public String generateBarChart(
            @P("对比维度：region（按大区对比）或 rep（按销售员对比）") String dimension,
            @P("查询开始日期，格式 yyyy-MM-dd") String startDate,
            @P("查询结束日期，格式 yyyy-MM-dd") String endDate,
            @P("图表标题") String title) {

        log.info("工具调用-generateBarChart: dim={}, start={}, end={}", dimension, startDate, endDate);

        try {
            ToolInputValidator.DateRange dates = validator.validateDateRange(startDate, endDate);
            String validDimension = validator.validateDimension(dimension, Set.of("region", "rep"));
            String validTitle = validator.validateTitle(title);
            DataScope scope = UserContext.requireDataScope();

            List<String> names;
            List<Number> values;

            if ("region".equals(validDimension)) {
                List<RegionSalesDTO> regions = queryService.queryRegionRanking(scope, dates.start(), dates.end());
                names = regions.stream().map(RegionSalesDTO::regionName).toList();
                values = regions.stream()
                        .map(r -> (Number) r.totalAmount().longValue()).toList();
            } else { // 维度是销售员的话按照下面处理
                List<com.dyh.salesAgent.dto.RepSalesDTO> reps =
                        queryService.queryRepRanking(scope, null, dates.start(), dates.end(), 10);
                names = reps.stream().map(r -> r.repName()).toList();
                values = reps.stream()
                        .map(r -> (Number) r.totalAmount().longValue()).toList();
            }

            if (names.isEmpty()) {
                return "暂无数据，无法生成图表";
            }

            Map<String, Object> option = new LinkedHashMap<>();
            option.put("title", Map.of("text", validTitle != null ? validTitle : "销售对比"));
            option.put("tooltip", Map.of("trigger", "axis"));
            option.put("xAxis", Map.of("type", "category", "data", names,
                    "axisLabel", Map.of("rotate", 30)));
            option.put("yAxis", Map.of("type", "value", "name", "销售额（元）"));
            option.put("series", List.of(Map.of(
                    "type", "bar",// 柱状图
                    "data", values,
                    "itemStyle", Map.of("color", "#91cc75")
            )));

            String json = objectMapper.writeValueAsString(option);
            return "CHART_JSON:" + json;

        } catch (IllegalArgumentException e) {
            return "参数无效：" + e.getMessage();
        } catch (Exception e) {
            log.error("生成柱状图失败", e);
            return "生成图表数据时出现问题，请稍后重试";
        }
    }

    @Tool("生成销售占比饼图的 ECharts JSON。适用于：画饼图、各部分占比、" +
         "份额分布等可视化需求。")
    public String generatePieChart(
            @P("饼图维度：region（大区占比）、category（品类占比）") String dimension,
            @P("查询开始日期，格式 yyyy-MM-dd") String startDate,
            @P("查询结束日期，格式 yyyy-MM-dd") String endDate,
            @P("图表标题") String title) {

        log.info("工具调用-generatePieChart: dim={}, start={}, end={}", dimension, startDate, endDate);

        try {
            ToolInputValidator.DateRange dates = validator.validateDateRange(startDate, endDate);
            String validDimension = validator.validateDimension(dimension, Set.of("region", "category"));
            String validTitle = validator.validateTitle(title);
            DataScope scope = UserContext.requireDataScope();

            List<Map<String, Object>> pieData; // 饼图的数据结构

            if ("region".equals(validDimension)) {
                List<RegionSalesDTO> regions = queryService.queryRegionRanking(scope, dates.start(), dates.end());
                pieData = regions.stream().map(r -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", r.regionName());
                    item.put("value", r.totalAmount().longValue());
                    return item;
                }).toList();
            } else {
                // 按产品品类汇总销售额。
                List<ProductSalesDTO> products = queryService.queryProductRanking(scope, dates.start(), dates.end(), 100);
                Map<String, BigDecimal> categoryMap = new LinkedHashMap<>();
                for (ProductSalesDTO p : products) {
                    categoryMap.merge(p.category(), p.totalAmount(), BigDecimal::add);
                }
                pieData = categoryMap.entrySet().stream().map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue().longValue());
                    return item;
                }).toList();
            }

            if (pieData.isEmpty()) {
                return "暂无数据，无法生成图表";
            }

            Map<String, Object> option = new LinkedHashMap<>();
            option.put("title", Map.of("text", validTitle != null ? validTitle : "销售占比", "left", "center"));
            option.put("tooltip", Map.of("trigger", "item", "formatter", "{b}: {c} ({d}%)"));
            option.put("legend", Map.of("orient", "vertical", "left", "left"));
            option.put("series", List.of(Map.of(
                    "type", "pie", // 饼图
                    "radius", "55%", // 饼图的半径
                    "data", pieData,
                    "emphasis", Map.of("itemStyle",
                            Map.of("shadowBlur", 10, "shadowOffsetX", 0, "shadowColor", "rgba(0,0,0,0.5)"))
            )));

            String json = objectMapper.writeValueAsString(option);
            return "CHART_JSON:" + json;

        } catch (IllegalArgumentException e) {
            return "参数无效：" + e.getMessage();
        } catch (Exception e) {
            log.error("生成饼图失败", e);
            return "生成图表数据时出现问题，请稍后重试";
        }
    }
}
