package com.dyh.salesAgent.dto;
// 销售汇总结果 DTO。
import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderSummaryDTO(
        String orderNo,
        String repName,
        String customerName,
        BigDecimal amount,
        String status,
        LocalDate orderDate
) {}
