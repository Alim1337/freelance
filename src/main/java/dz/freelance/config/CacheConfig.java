package dz.freelance.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableCaching
public class CacheConfig {

    // Cache en mémoire — fonctionne sans Redis
    // En production (Railway), Redis sera utilisé automatiquement
    // via spring.data.redis.url et spring.cache.type=redis
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "categories-tree",
            "categories-flat",
            "services",
            "providers"
        );
    }
}
