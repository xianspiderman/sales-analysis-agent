package com.dyh.salesAgent.service;


import com.dyh.salesAgent.security.DataScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

/** 手动 Redis 缓存仅负责多参数总销售额；排名和趋势继续由 SalesQueryService 的 @Cacheable 负责。 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SalesQueryCacheService {

    private final SalesQueryService queryService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${sales-agent.cache.query-ttl-seconds:300}")
    private long cacheTtlSeconds;

    public BigDecimal queryTotalAmountCached(DataScope scope, Long regionId, Long repId,
                                             LocalDate start, LocalDate end) {
        SalesQueryService.QueryFilters filters = queryService.validateScopeFilters(scope, regionId, repId); // 合法性检查
        regionId = filters.regionId();
        repId = filters.repId();
        String cacheKey = String.format("sales:total:%s:%s:%s:%s:%s",
                scope.cacheKey(), idPart(regionId), idPart(repId), start, end);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("销售额缓存命中: key={}", cacheKey);
            return new BigDecimal(cached.toString());
        }

        log.debug("销售额缓存未命中: key={}", cacheKey);
        BigDecimal result = queryService.queryTotalAmount(scope, regionId, repId, start, end);
        redisTemplate.opsForValue().set(cacheKey, result.toPlainString(), Duration.ofSeconds(cacheTtlSeconds));
        return result;
    }

    private String idPart(Long id) {
        return id == null ? "all" : id.toString();
    }
}
