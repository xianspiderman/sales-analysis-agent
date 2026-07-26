package com.dyh.salesAgent.dto;
// OrderSummaryDTO.java（工具返回用，这节实际用 String，DTO 留给后续）
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
