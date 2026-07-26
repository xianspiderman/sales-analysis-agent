package com.dyh.salesAgent.repository;
import com.dyh.salesAgent.entity.SalesOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    // 通用数据权限条件
    // scopeType=COMPANY：总监查看全公司；scopeId 固定传 0，此分支不限制 region_id/rep_id。
    // scopeType=REGION：主管只能查看 o.regionId 等于自己大区 ID 的订单。
    // scopeType=REP：销售员只能查看 o.repId 等于自己销售员 ID 的订单。
    // 三个分支虽然用 OR 连接，但 scopeType 一次只会等于一个值，因此实际只会命中一个权限分支。
    String SCOPE = "(:scopeType = 'COMPANY' OR " +
            "(:scopeType = 'REGION' AND o.regionId = :scopeId) OR " +
            "(:scopeType = 'REP' AND o.repId = :scopeId)) ";

    // 基础订单查询
    // 按销售员查
    // 按大区查
    // 按产品查
    // 查询指定日期范围内、当前用户有权查看的订单列表。
    @Query("SELECT o FROM SalesOrder o WHERE " + SCOPE +
            "AND (:regionId IS NULL OR o.regionId = :regionId) " +
            "AND (:repId IS NULL OR o.repId = :repId) " +
            "AND o.orderDate BETWEEN :start AND :end ORDER BY o.orderDate DESC, o.id DESC")
    List<SalesOrder> findOrders(@Param("scopeType") String scopeType,
                                @Param("scopeId") Long scopeId,
                                @Param("regionId") Long regionId,
                                @Param("repId") Long repId,
                                @Param("start") LocalDate start,
                                @Param("end") LocalDate end,
                                Pageable pageable);

    
    // 销售额汇总
    // 某大区某时段的完成订单总金额
    // 某销售员某时段的完成订单总金额
    // 查询当前权限范围、可选大区/销售员筛选和指定时段内的已完成订单总金额。
    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM SalesOrder o WHERE " + SCOPE +
            "AND (:regionId IS NULL OR o.regionId = :regionId) " +
            "AND (:repId IS NULL OR o.repId = :repId) " +
            "AND o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end")
    BigDecimal sumAmount(@Param("scopeType") String scopeType,
                         @Param("scopeId") Long scopeId,
                         @Param("regionId") Long regionId,
                         @Param("repId") Long repId,
                         @Param("start") LocalDate start,
                         @Param("end") LocalDate end);

    
    // 排名查询
    // 各销售员业绩排名
    // 当前实现可在当前数据权限范围内进一步按大区过滤。
    @Query("SELECT o.repId, SUM(o.amount) AS total FROM SalesOrder o WHERE " + SCOPE +
            "AND (:regionId IS NULL OR o.regionId = :regionId) " +
            "AND o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end " +
            "GROUP BY o.repId ORDER BY total DESC")
    List<Object[]> findRepRanking(@Param("scopeType") String scopeType,
                                  @Param("scopeId") Long scopeId,
                                  @Param("regionId") Long regionId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end,
                                  Pageable pageable);

    // 各大区业绩排名
    // 总监会得到多个大区；主管只会得到本大区；销售员只会汇总本人订单所属的大区。
    // 每个 Object[] 是 [regionId, total]。
    @Query("SELECT o.regionId, SUM(o.amount) AS total FROM SalesOrder o WHERE " + SCOPE +
            "AND o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end " +
            "GROUP BY o.regionId ORDER BY total DESC")
    List<Object[]> findRegionRanking(@Param("scopeType") String scopeType,
                                     @Param("scopeId") Long scopeId,
                                     @Param("start") LocalDate start,
                                     @Param("end") LocalDate end);

    // 各产品销售排名
    // 当前查询按销售额从高到低排列。
    // 每个 Object[] 是 [productId, total, qty]，分别表示产品 ID、销售额、销售数量。
    @Query("SELECT o.productId, SUM(o.amount) AS total, SUM(o.quantity) AS qty " +
            "FROM SalesOrder o WHERE " + SCOPE +
            "AND o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end " +
            "GROUP BY o.productId ORDER BY total DESC")
    List<Object[]> findProductRanking(@Param("scopeType") String scopeType,
                                      @Param("scopeId") Long scopeId,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end,
                                      Pageable pageable);

    // 滞销产品排名（销售额从低到高）。
    // 单独建立此查询是为了让数据库直接返回“最差 TopN”，避免先查询大量产品再在 Java 中倒序截取。
    @Query("SELECT o.productId, SUM(o.amount) AS total, SUM(o.quantity) AS qty " +
            "FROM SalesOrder o WHERE " + SCOPE +
            "AND o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end " +
            "GROUP BY o.productId ORDER BY total ASC")
    List<Object[]> findWorstProductRanking(@Param("scopeType") String scopeType,
                                           @Param("scopeId") Long scopeId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end,
                                           Pageable pageable);

    
    // 趋势分析
    // 月度汇总（用于趋势分析）
    @Query(value = "SELECT DATE_FORMAT(order_date, '%Y-%m') AS month, " +
            "SUM(amount) AS total, COUNT(*) AS order_count FROM sa_sales_order " +
            "WHERE status = 'COMPLETED' " +
            "AND (:scopeType = 'COMPANY' OR (:scopeType = 'REGION' AND region_id = :scopeId) " +
            "OR (:scopeType = 'REP' AND rep_id = :scopeId)) " +
            "AND (:regionId IS NULL OR region_id = :regionId) " +
            "AND order_date BETWEEN :start AND :end GROUP BY month ORDER BY month",
            nativeQuery = true)
    List<Object[]> findMonthlyTrend(@Param("scopeType") String scopeType,
                                    @Param("scopeId") Long scopeId,
                                    @Param("regionId") Long regionId,
                                    @Param("start") LocalDate start,
                                    @Param("end") LocalDate end);

    
    // 异常检测辅助查询
    // 产品最近一次出单日期（用于预警）
    // 查询某产品在当前用户可见数据范围内的最后一次已完成订单日期。
    // 总监看到全公司最后出单日，主管看到本区最后出单日，销售员只看到本人最后出单日。
    @Query("SELECT MAX(o.orderDate) FROM SalesOrder o WHERE " + SCOPE +
            "AND o.productId = :productId AND o.status = 'COMPLETED'")
    LocalDate findLastOrderDateByProduct(@Param("scopeType") String scopeType,
                                         @Param("scopeId") Long scopeId,
                                         @Param("productId") Long productId);

    // 某销售员的退单率统计
    @Query("SELECT o.repId, SUM(CASE WHEN o.status = 'REFUNDED' THEN 1 ELSE 0 END), COUNT(o) " +
            "FROM SalesOrder o WHERE " + SCOPE +
            "AND o.orderDate BETWEEN :start AND :end GROUP BY o.repId")
    List<Object[]> findRefundRateByRep(@Param("scopeType") String scopeType,
                                       @Param("scopeId") Long scopeId,
                                       @Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    // 某大区某时段的订单数（用于异常检测）
    // 当前只统计已完成订单，用于大区订单量骤降检测。
    // regionId 为 null 时只按 DataScope 查询；非 null 时在权限范围内继续限定该大区。
    @Query("SELECT COUNT(o) FROM SalesOrder o WHERE " + SCOPE +
            "AND (:regionId IS NULL OR o.regionId = :regionId) " +
            "AND o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end")
    long countCompleted(@Param("scopeType") String scopeType,
                        @Param("scopeId") Long scopeId,
                        @Param("regionId") Long regionId,
                        @Param("start") LocalDate start,
                        @Param("end") LocalDate end);
}
