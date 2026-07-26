package com.dyh.salesAgent.controller;
public record ChatResponse(
        String sessionId,
        String reply,
        long durationMs // 耗时
) {}
