package com.dyh.salesAgent.config;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching // @EnableCaching开启缓存机制->@Cacheable声明缓存方法->Spring创建缓存代理->RedisCacheManager执行Redis操作
public class RedisConfig {

    /**
     * 创建带类型信息的 ObjectMapper。
     * activateDefaultTyping 让 Jackson 在序列化时写入 @class 字段，
     * 反序列化时才能还原成正确的 Java 类型（而不是 LinkedHashMap）。
     */
    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // WRAPPER_ARRAY(包装数组) 将类型信息包在数组里：["com.example.Dto", {...}](["具体Java类型", {"实际数据": "..."}])
        // 比 AS_PROPERTY 对 List/集合类型更兼容
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.WRAPPER_ARRAY
        );
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) { // RedisConnectionFactory自带
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper()); // json序列化

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer()); // Hash字段Key保存为可读字符串
        template.setHashValueSerializer(jsonSerializer); // Hash字段Value保存为带Java类型信息的JSON
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) { // 缓存管理器
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper()); // 声明json序列化的逻辑

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // 默认5分钟的缓存
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer));

        // 不同缓存区设置不同 TTL
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("rep-ranking",       defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("region-ranking",    defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("product-ranking",   defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("monthly-trend",     defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("region-meta",       defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("anomaly-detection", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
