package ma.codexa.goldyara.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cache configuration. Provides an in-memory CacheManager for goldTypes, productTypes,
 * categories, collections and goldPriceSetting caches.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final List<String> CACHE_NAMES = List.of(
            "goldTypes",
            "productTypes",
            "categories",
            "collections",
            "goldPriceSetting"
    );

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(CACHE_NAMES);
        return cacheManager;
    }
}
