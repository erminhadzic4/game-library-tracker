package com.erminhadzic.gamelibrarytracker.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

// Switches on Spring's caching, so that @Cacheable methods (RawgClient's two discover lists) are remembered.
// The cache itself is created by Spring Boot from the spring.cache.* settings in application.properties.
@Configuration
@EnableCaching
public class CacheConfig {
}
