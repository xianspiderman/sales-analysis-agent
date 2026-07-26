package com.dyh.salesAgent.dto;
import java.math.BigDecimal;

public record RepSalesDTO(
        Long repId,
        String repName,
        Long regionId,
        String regionName,
        BigDecimal totalAmount,
        Integer orderCount //订单数量
) {}
