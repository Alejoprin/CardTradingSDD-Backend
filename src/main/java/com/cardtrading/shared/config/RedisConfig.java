package com.cardtrading.shared.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Mixin that teaches Jackson how to deserialize {@link PageImpl} directly.
     * The {@code @JsonCreator} constructor maps the JSON fields produced by
     * Spring's page serialization back into a {@code PageImpl} instance.
     * Without this, Jackson cannot construct {@code PageImpl} because it has
     * no default constructor.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    abstract static class PageImplMixin<T> {
        @JsonCreator
        PageImplMixin(
                @JsonProperty("content") java.util.List<T> content,
                @JsonProperty("number")  int page,
                @JsonProperty("size")    int size,
                @JsonProperty("totalElements") long totalElements) { }
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // Register a module that teaches Jackson how to deserialize PageImpl
        // directly via the PageImplMixin @JsonCreator constructor.
        SimpleModule pageModule = new SimpleModule("PageImplModule");
        pageModule.setMixInAnnotation(PageImpl.class, PageImplMixin.class);
        mapper.registerModule(pageModule);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(mapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .prefixCacheNameWith("cardtrading:");

        Map<String, RedisCacheConfiguration> cacheConfigurations = Map.of(
                "card:catalog", defaultConfig.entryTtl(Duration.ofHours(1)),
                "card:detail",  defaultConfig.entryTtl(Duration.ofHours(1)),
                "card:set",     defaultConfig.entryTtl(Duration.ofHours(1)),
                "games:all",    defaultConfig.entryTtl(Duration.ofHours(6)),
                "cardsets:all", defaultConfig.entryTtl(Duration.ofHours(6)),
                "user:profile", defaultConfig.entryTtl(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}