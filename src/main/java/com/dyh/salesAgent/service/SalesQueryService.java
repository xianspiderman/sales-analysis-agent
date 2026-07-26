package com.dyh.salesAgent.service;
import com.dyh.salesAgent.dto.*;
import com.dyh.salesAgent.entity.Product;
import com.dyh.salesAgent.entity.SalesOrder;
import com.dyh.salesAgent.entity.SalesRegion;
import com.dyh.salesAgent.entity.SalesRep;
import com.dyh.salesAgent.repository.*;
import com.dyh.salesAgent.security.DataScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalesQueryService {

    private final SalesOrderRepository orderRepository;
    private final SalesRepRepository repRepository;
    private final ProductRepository productRepository;
    private final SalesRegionRepository regionRepository;

    // ============================================================
    // 基础查询
    // ============================================================

    /**
     * 查询指定时段的订单列表
     * scope 是当前登录用户的强制数据范围；repId/regionId 是用户问题中可选的附加筛选。
     * limit 通过 PageRequest 下推到数据库，不再先加载全部订单后在 Java 中截取。
     */
    public List<SalesOrder> queryOrders(DataScope scope, Long repId, Long regionId,
                                        LocalDate start, LocalDate end, int limit) {
        Long safeRegion = allowedRegion(scope, regionId);
        Long safeRep = allowedRep(scope, repId);
        return orderRepository.findOrders(scope.type().name(), scope.scopeId(), safeRegion, safeRep,
                start, end, PageRequest.of(0, limit));
    }

    /**
     * 查询总销售额
     * 当前实现查询指定 DataScope、可选筛选和指定时段内已完成订单的总金额。
     */
    public BigDecimal queryTotalAmount(DataScope scope, Long regionId, Long repId,
                                       LocalDate start, LocalDate end) {
        QueryFilters filters = validateScopeFilters(scope, regionId, repId);
        return orderRepository.sumAmount(scope.type().name(), scope.scopeId(),
                filters.regionId(), filters.repId(), start, end);
    }

    // ============================================================
    // 排名查询
    // ============================================================

    /**
     * 销售员业绩排名（带姓名、大区信息）
     * 整体流程：数据库按权限聚合排名 → 批量加载排名中涉及的销售员 → 批量加载对应大区 → 组装 DTO。
     */
    // 排名数据缓存 5 分钟
    @Cacheable(value = "rep-ranking", // value是缓存区名称，对应RedisConfig中5分钟TTL。
            key = "#scope.cacheKey() + ':' + (#regionId == null ? 'all' : #regionId) + ':' + #start + ':' + #end + ':' + #topN")
    public List<RepSalesDTO> queryRepRanking(DataScope scope, Long regionId,
                                             LocalDate start, LocalDate end, int topN) {
        Long safeRegion = allowedRegion(scope, regionId);
        // 第一次查询：数据库在权限范围内按 repId 分组、按销售额降序，并通过 PageRequest 限制 TopN。
        List<Object[]> raw = orderRepository.findRepRanking(scope.type().name(), scope.scopeId(), safeRegion,
                start, end, PageRequest.of(0, topN));
        Set<Long> repIds = raw.stream().map(row -> ((Number) row[0]).longValue()).collect(Collectors.toSet());
        // 第二次查询：一次性加载排名中涉及的销售员并转成 id -> SalesRep 的 Map，避免循环里逐个查人造成 N+1。
        Map<Long, SalesRep> reps = repRepository.findAllById(repIds).stream()
                .collect(Collectors.toMap(SalesRep::getId, Function.identity()));
        Set<Long> regionIds = reps.values().stream().map(SalesRep::getRegionId).collect(Collectors.toSet());
        // 第三次查询：一次性加载相关大区并转成 regionId -> regionName 的 Map。
        Map<Long, String> regions = regionRepository.findAllById(regionIds).stream()
                .collect(Collectors.toMap(SalesRegion::getId, SalesRegion::getName));
        return raw.stream().map(row -> {
            // row[0] 是数据库返回的 repId；JPA 聚合结果类型用 Number 接收后再转 long 最稳妥。
            Long id = ((Number) row[0]).longValue();
            SalesRep rep = reps.get(id);
            // 这里 orderCount 需要单独查，简化处理用 0
            return rep == null ? null : new RepSalesDTO(id, rep.getName(), rep.getRegionId(),
                    regions.getOrDefault(rep.getRegionId(), "未知"), decimal(row[1]), 0);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 大区业绩排名
     */
    @Cacheable(value = "region-ranking", key = "#scope.cacheKey() + ':' + #start + ':' + #end")
    public List<RegionSalesDTO> queryRegionRanking(DataScope scope, LocalDate start, LocalDate end) {
        // 第一次查询：在当前权限范围内按 regionId 汇总完成订单金额。
        List<Object[]> raw = orderRepository.findRegionRanking(scope.type().name(), scope.scopeId(), start, end);
        Set<Long> ids = raw.stream().map(row -> ((Number) row[0]).longValue()).collect(Collectors.toSet());
        // 第二次查询：批量加载大区名称，避免遍历排名时逐条查询。
        Map<Long, String> names = regionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(SalesRegion::getId, SalesRegion::getName));
        return raw.stream().map(row -> new RegionSalesDTO(((Number) row[0]).longValue(),
                names.getOrDefault(((Number) row[0]).longValue(), "未知"), decimal(row[1]), 0, BigDecimal.ZERO))
                .collect(Collectors.toList());
    }

    /**
     * 产品销售排名
     * 该三参数版本默认查询销售额从高到低的畅销产品。
     */
    @Cacheable(value = "product-ranking", key = "#scope.cacheKey() + ':' + #start + ':' + #end + ':' + #topN")
    public List<ProductSalesDTO> queryProductRanking(DataScope scope, LocalDate start, LocalDate end, int topN) {
        return queryProductRanking(scope, start, end, topN, false);
    }

    /**
     * 产品销售排名。
     * worst=false 查询畅销 TopN；worst=true 查询滞销 TopN。
     */
    @Cacheable(value = "product-ranking",
            key = "#scope.cacheKey() + ':' + #start + ':' + #end + ':' + #topN + ':' + #worst")
    public List<ProductSalesDTO> queryProductRanking(DataScope scope, LocalDate start, LocalDate end,
                                                     int topN, boolean worst) {
        // 第一次查询：根据 worst 选择数据库降序或升序排名，两种查询都包含同一个 DataScope 权限条件。
        List<Object[]> raw = worst
                ? orderRepository.findWorstProductRanking(scope.type().name(), scope.scopeId(), start, end,
                        PageRequest.of(0, topN))
                : orderRepository.findProductRanking(scope.type().name(), scope.scopeId(), start, end,
                        PageRequest.of(0, topN));
        Set<Long> ids = raw.stream().map(row -> ((Number) row[0]).longValue()).collect(Collectors.toSet());
        // 第二次查询：批量加载产品资料并构造 productId -> Product Map，避免 N+1。
        Map<Long, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return raw.stream().map(row -> {
            Long id = ((Number) row[0]).longValue();
            Product p = products.get(id);
            return p == null ? null : new ProductSalesDTO(id, p.getSkuCode(), p.getName(), p.getCategory(),
                    decimal(row[1]), ((Number) row[2]).intValue());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // ============================================================
    // 趋势分析
    // ============================================================

    /**
     * 月度趋势数据（近 N 个月）
     */
    @Cacheable(value = "monthly-trend",
            key = "#scope.cacheKey() + ':' + (#regionId == null ? 'all' : #regionId) + ':' + #months")
    public List<MonthlyTrendDTO> queryMonthlyTrend(DataScope scope, Long regionId, int months) {
        Long safeRegion = allowedRegion(scope, regionId);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(months - 1L).withDayOfMonth(1);
        return orderRepository.findMonthlyTrend(scope.type().name(), scope.scopeId(), safeRegion, start, end).stream()
                .map(row -> new MonthlyTrendDTO(row[0].toString(), decimal(row[1]), ((Number) row[2]).intValue()))
                .collect(Collectors.toList());
    }

    /**
     * 计算环比增长率（当期 vs 上期）
     * 计算公式：(当期金额 - 上期金额) / 上期金额 × 100%。
     */
    public BigDecimal calcGrowthRate(BigDecimal current, BigDecimal previous) {
        // 上期为 null 或 0 时没有可用分母，返回 null 表示无法计算，而不是伪造 0%。
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) return null;
        return current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 异常检测辅助
    // ============================================================

    /**
     * 查询产品最后一次出单日期
     * 当前实现只查询 DataScope 内该产品最后一次完成订单的日期。
     */
    public LocalDate queryLastOrderDate(DataScope scope, Long productId) {
        return orderRepository.findLastOrderDateByProduct(scope.type().name(), scope.scopeId(), productId);
    }

    /**
     * 查询大区在指定时段内的订单数
     * 当前实现查询 DataScope 内某大区在指定时段的已完成订单数。
     */
    public long queryOrderCount(DataScope scope, Long regionId, LocalDate start, LocalDate end) {
        return orderRepository.countCompleted(scope.type().name(), scope.scopeId(), allowedRegion(scope, regionId), start, end);
    }

    /**
     * 查询所有销售员退单率
     * “所有”是指当前 DataScope 有权查看的销售员，不是无条件查询全公司。
     */
    public List<Object[]> queryRefundRates(DataScope scope, LocalDate start, LocalDate end) {
        return orderRepository.findRefundRateByRep(scope.type().name(), scope.scopeId(), start, end);
    }

    /**
     * 返回当前用户有权参与异常扫描的大区列表。
     */
    public List<SalesRegion> visibleRegions(DataScope scope) {
        return switch (scope.type()) {
            // 总监扫描全部大区。
            case COMPANY -> regionRepository.findAll();
            // 主管只加载自己负责的大区。
            case REGION -> regionRepository.findAllById(List.of(scope.scopeId()));
            // 销售员先用 repId 找到本人，再加载本人所属大区。
            case REP -> repRepository.findById(scope.scopeId())
                    .map(rep -> regionRepository.findAllById(List.of(rep.getRegionId()))).orElseGet(List::of);
        };
    }

    /**
     * 返回当前用户有权参与异常扫描的普通销售员列表。
     */
    public List<SalesRep> visibleSalesReps(DataScope scope) {
        return switch (scope.type()) {
            // 总监加载全公司的普通销售员，不把主管和总监混入个人业绩异常扫描。
            case COMPANY -> repRepository.findByRole("SALES_REP");
            // 主管只加载本大区普通销售员。
            case REGION -> repRepository.findByRoleAndRegionId("SALES_REP", scope.scopeId());
            // 普通销售员只加载本人。
            case REP -> repRepository.findAllById(List.of(scope.scopeId()));
        };
    }

    // ============================================================
    // 辅助查询（名称解析），比如根据员工id查找员工姓名等
    // ============================================================

    /** 根据销售员 ID 查询姓名，找不到时返回“未知销售员”。 */
    public String getRepName(Long repId) {
        return repRepository.findById(repId).map(SalesRep::getName).orElse("未知销售员");
    }

    // 大区名称 → ID 映射缓存（几乎不变）
    // 大区元数据不随用户权限变化，因此可以跨用户复用。
    @Cacheable(value = "region-meta", key = "#regionName")
    public Long getRegionIdByName(String regionName) {
        return regionRepository.findByName(regionName).map(SalesRegion::getId).orElse(null);
    }

    /** 根据销售员姓名解析销售员 ID。 */
    public Long getRepIdByName(String repName) {
        return repRepository.findByName(repName).map(SalesRep::getId).orElse(null);
    }

    // ============================================================
    // 主动筛选条件的权限检查
    // ============================================================


    /**
     * 同时校验用户问题中可选的 regionId 和 repId，并返回已经确认安全的筛选对象。
     */
    public QueryFilters validateScopeFilters(DataScope scope, Long regionId, Long repId) {
        return new QueryFilters(allowedRegion(scope, regionId), allowedRep(scope, repId));
    }

    public record QueryFilters(Long regionId, Long repId) {}

    /**
     * 检查用户主动指定的大区是否在当前 DataScope 内。
     */
    private Long allowedRegion(DataScope scope, Long requested) {
        // 没有主动指定大区时不额外筛选，但 Repository 仍会执行 DataScope 强制权限条件。
        if (requested == null) return null;
        // 主管只能指定自己负责的大区。
        if (scope.type() == DataScope.ScopeType.REGION && !scope.scopeId().equals(requested)) {
            throw new IllegalArgumentException("无权查询其他大区数据");
        }
        // 销售员的 DataScope 只保存 repId，因此先查询本人记录，再确认 requested 是本人所属大区。
        if (scope.type() == DataScope.ScopeType.REP) {
            SalesRep rep = repRepository.findById(scope.scopeId())
                    .orElseThrow(() -> new IllegalStateException("当前销售员身份不存在"));
            if (!rep.getRegionId().equals(requested)) throw new IllegalArgumentException("无权查询其他大区数据");
        }
        return requested;
    }

    /**
     * 检查用户主动指定的销售员是否在当前 DataScope 内。
     */
    private Long allowedRep(DataScope scope, Long requested) {
        // 没有主动指定销售员时不额外筛选，仍保留 DataScope 强制权限。
        if (requested == null) return null;
        // 普通销售员只能明确查询本人。
        if (scope.type() == DataScope.ScopeType.REP && !scope.scopeId().equals(requested)) {
            throw new IllegalArgumentException("无权查询其他销售员数据");
        }
        // 主管明确指定销售员时，先确认该销售员确实属于主管的大区。
        if (scope.type() == DataScope.ScopeType.REGION) {
            SalesRep rep = repRepository.findById(requested)
                    .orElseThrow(() -> new IllegalArgumentException("销售员不存在"));
            if (!scope.scopeId().equals(rep.getRegionId())) throw new IllegalArgumentException("无权查询其他大区销售员数据");
        }
        return requested;
    }

    /** 把 JPA 聚合查询返回的 Number/Decimal 安全转换成 BigDecimal。 */
    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }
}
