package com.dyh.salesAgent.dto;
import java.math.BigDecimal;

public record RegionSalesDTO(
        Long regionId,
        String regionName,
        BigDecimal totalAmount,
        Integer orderCount, //订单数量
        BigDecimal totalProfit
) {}
