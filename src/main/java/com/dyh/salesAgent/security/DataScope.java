package com.dyh.salesAgent.security;
/**
 * 当前登录用户允许查询的“数据边界”，它不是销售数据本身，也不负责执行查询。
 * Tool 先得到 DataScope，再把它显式传给 Service、Repository 和缓存层，最终变成 SQL 查询条件与缓存 Key。
 * scopeId 在 COMPANY 下固定为 0，在 REGION/REP 下分别是大区 ID/销售员 ID。
 */
public record DataScope(ScopeType type, Long scopeId) {

    // COMPANY=全公司，REGION=一个大区，REP=一个销售员本人。
    public enum ScopeType { COMPANY, REGION, REP }

    // record 的紧凑构造器：任何不完整或非法的数据范围都不能被创建。
    public DataScope {
        if (type == null || scopeId == null) {
            throw new IllegalArgumentException("数据权限范围不能为空");
        }
        if (type != ScopeType.COMPANY && scopeId <= 0) {
            throw new IllegalArgumentException("数据权限范围 ID 无效");
        }
    }

    public static DataScope from(UserContext.UserInfo user) {
        if (user == null) { // 先确认用户存在，再做角色映射
            throw new IllegalStateException("用户上下文缺失，拒绝访问销售数据");
        }
        // 同一个角色只会得到一种确定的数据边界，避免每个 Tool 各写一套角色判断。
        return switch (user.role()) {
            case "SALES_DIRECTOR" -> new DataScope(ScopeType.COMPANY, 0L);
            case "SALES_MANAGER" -> new DataScope(ScopeType.REGION, requireId(user.regionId(), "大区"));
            case "SALES_REP" -> new DataScope(ScopeType.REP, requireId(user.repId(), "销售员"));
            default -> throw new IllegalStateException("无法识别当前用户角色，拒绝访问销售数据");
        };
    }

    public String cacheKey() {
        return type.name() + ":" + scopeId;
    }

    private static Long requireId(Long id, String label) {
        if (id == null || id <= 0) {
            throw new IllegalStateException(label + "权限范围缺失，拒绝访问销售数据");
        }
        return id;
    }
}
