package com.dyh.salesAgent.tool;
import com.dyh.salesAgent.dto.AnomalyDTO;
import com.dyh.salesAgent.entity.Product;
import com.dyh.salesAgent.entity.SalesRegion;
import com.dyh.salesAgent.entity.SalesRep;
import com.dyh.salesAgent.repository.ProductRepository;
import com.dyh.salesAgent.security.DataScope;
import com.dyh.salesAgent.service.SalesQueryService;
import com.dyh.salesAgent.security.UserContext;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionTool {

    private final SalesQueryService queryService;
    private final ProductRepository productRepository;
    @Value("${sales-agent.tool.anomaly-threshold-days:5}")
    private int zeroSaleThresholdDays;// 超过N天（默认 5 天），比如某产品最后一次出单距今天数

    @Value("${sales-agent.tool.trend-drop-threshold:0.3}")
    private double trendDropThreshold;// 下降超过配置的threshold（默认30%）就预警

    @Tool("自动检测销售数据中的所有异常，包括：大区订单量骤降、产品连续零销售、" +
            "销售员退单率异常、销售员业绩骤降。适用于：有没有异常、风险排查、预警检测等场景。" +
            "无需传入参数，系统根据当前用户权限自动扫描可见范围内的数据。")// 这里加入了"根据当前用户权限自动扫描"的描述
    public String detectAllAnomalies() {

        DataScope scope = UserContext.requireDataScope();

        log.info("工具调用-detectAllAnomalies: scope={}", scope.cacheKey());

        List<AnomalyDTO> anomalies = new ArrayList<>();

        try {// 下面这些是把异常类型都扔进去
            switch (scope.type()) {
                case COMPANY, REGION -> {
                    anomalies.addAll(detectRegionDropAnomalies(scope));
                    anomalies.addAll(detectZeroSaleProducts(scope));
                    anomalies.addAll(detectHighRefundReps(scope));
                    anomalies.addAll(detectRepPerformanceDrop(scope));
                }
                case REP -> {
                    anomalies.addAll(detectHighRefundReps(scope));
                    anomalies.addAll(detectRepPerformanceDrop(scope));
                }
            }
        } catch (Exception e) {
            log.error("异常检测出错", e);
            return "异常检测过程中出现问题，请稍后重试";
        }

        if (anomalies.isEmpty()) {
            return "当前数据未检测到明显异常，您可见范围内的销售数据运行正常。";
        }

        // 按优先级排序：HIGH > MEDIUM > LOW
        anomalies.sort((a, b) -> severityOrder(a.severity()) - severityOrder(b.severity()));

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("异常检测结果：共发现 %d 个异常\n\n", anomalies.size()));

        for (AnomalyDTO anomaly : anomalies) {
            String icon = switch (anomaly.severity()) {
                case "HIGH"   -> "🔴 高优先级";
                case "MEDIUM" -> "🟡 中优先级";
                default       -> "🔵 低优先级";
            };
            sb.append(String.format("%s｜%s\n", icon, anomaly.type()));
            sb.append(String.format("  对象：%s\n", anomaly.subject()));
            sb.append(String.format("  描述：%s\n", anomaly.description()));
            sb.append(String.format("  建议：%s\n\n", anomaly.suggestion()));
        }

        return sb.toString();
    }

    // 检测一：大区订单量骤降（filterRegionId 为 null 表示扫描全部大区）
    private List<AnomalyDTO> detectRegionDropAnomalies(DataScope scope) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        // 近2周vs过去4周每2周的平均
        LocalDate recentStart = today.minusWeeks(2);
        LocalDate baseStart = today.minusWeeks(6);
        LocalDate baseEnd = today.minusWeeks(2).minusDays(1);// 上面两行过去4周

        List<SalesRegion> regions = queryService.visibleRegions(scope);

        for (SalesRegion region : regions) {
            long recentCount = queryService.queryOrderCount(scope, region.getId(), recentStart, today);
            long baseCount = queryService.queryOrderCount(scope, region.getId(), baseStart, baseEnd);
            double baseAvg = baseCount / 2.0;// 基准期 4 周折算成每 2 周平均
            if (baseAvg < 2) continue;// 样本量太小，忽略

            double dropRate = (baseAvg - recentCount) / baseAvg;
            if (dropRate > trendDropThreshold) {
                String severity = dropRate > 0.6 ? "HIGH" : "MEDIUM";
                result.add(new AnomalyDTO("大区订单量骤降", severity, region.getName(),
                        String.format("近 2 周订单量 %d 笔，过去 4 周均值 %.1f 笔/两周，下降 %.0f%%",
                                recentCount, baseAvg, dropRate * 100),
                        "建议联系大区负责人确认原因"));
            }
        }
        return result;
    }

    // 检测二：产品连续零销售（按大区过滤）
    private List<AnomalyDTO> detectZeroSaleProducts(DataScope scope) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Product product : productRepository.findByStatus("ACTIVE")) {
            LocalDate lastSaleDate = queryService.queryLastOrderDate(scope, product.getId());
            if (lastSaleDate == null) continue;// 从未销售的新品，跳过

            long days = ChronoUnit.DAYS.between(lastSaleDate, today);
            if (days >= zeroSaleThresholdDays) {
                String severity = days >= 14 ? "HIGH" : days >= 7 ? "MEDIUM" : "LOW";
                result.add(new AnomalyDTO("产品连续零销售", severity,
                        product.getName() + "（" + product.getSkuCode() + "）",
                        String.format("已连续 %d 天无销售订单", days),
                        "检查产品是否下架、库存是否充足"));
            }
        }
        return result;
    }

    // 检测三：销售员退单率异常（按大区过滤）
    private List<AnomalyDTO> detectHighRefundReps(DataScope scope) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);

        List<Object[]> refundData = queryService.queryRefundRates(scope, start, end);

        for (Object[] row : refundData) {
            Long rid = ((Number) row[0]).longValue();
            long refunded = ((Number) row[1]).longValue();
            long total = ((Number) row[2]).longValue();
            if (total < 3) continue;// 样本量太小

            double refundRate = (double) refunded / total;
            if (refundRate > 0.15) {
                String repName = queryService.getRepName(rid);
                result.add(new AnomalyDTO("销售员退单率异常",
                        refundRate > 0.3 ? "HIGH" : "MEDIUM", repName,
                        String.format("近 30 天退单率 %.0f%%（%d/%d 单）", refundRate * 100, refunded, total),
                        "建议沟通了解原因"));
            }
        }
        return result;
    }

    // 检测四：销售员业绩骤降（按大区过滤）
    private List<AnomalyDTO> detectRepPerformanceDrop(DataScope scope) {
        List<AnomalyDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate curStart = today.minusDays(30);
        LocalDate prevStart = today.minusDays(60);
        LocalDate prevEnd = today.minusDays(31);

        List<SalesRep> reps = queryService.visibleSalesReps(scope);

        for (SalesRep rep : reps) {
            BigDecimal current = queryService.queryTotalAmount(scope, null, rep.getId(), curStart, today);
            BigDecimal previous = queryService.queryTotalAmount(scope, null, rep.getId(), prevStart, prevEnd);
            if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) continue;
            if (current == null) current = BigDecimal.ZERO;

            double dropRate = previous.subtract(current)
                    .divide(previous, 4, BigDecimal.ROUND_HALF_UP).doubleValue();
            if (dropRate > 0.4) {
                result.add(new AnomalyDTO("销售员业绩骤降",
                        dropRate > 0.7 ? "HIGH" : "MEDIUM", rep.getName(),
                        String.format("近 30 天 ¥%.0f，上期 ¥%.0f，下降 %.0f%%", current, previous, dropRate * 100),
                        "建议跟进确认原因"));
            }
        }
        return result;
    }

    private int severityOrder(String severity) {
        return switch (severity) { case "HIGH" -> 0; case "MEDIUM" -> 1; default -> 2; };
    }
}
