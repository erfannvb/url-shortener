package nvb.dev.urlshortener;

import nvb.dev.urlshortener.domain.ShortUrl;
import nvb.dev.urlshortener.repository.UrlRepository;
import nvb.dev.urlshortener.service.CachedShortUrlService;
import nvb.dev.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles(value = "test")
public class CachedShortUrlRedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private CachedShortUrlService cachedShortUrlService;

    @Autowired
    private UrlService urlService;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        urlRepository.deleteAll();

        Cache cache = cacheManager.getCache("shortUrls");
        if (cache != null)
            cache.clear();
    }

    @Test
    void getOriginalUrl_whenCacheMisses_fetchesFromDatabaseAndStoresInRedis() {
        ShortUrl shortUrl = MotherObject.validShortUrlForIntegrationTest();
        urlRepository.save(shortUrl);

        Optional<String> originalUrl = cachedShortUrlService.getOriginalUrl(shortUrl.getShortCode());

        assertThat(originalUrl).isEqualTo(shortUrl.getOriginalUrl());

        Cache cache = cacheManager.getCache("shortUrls");
        assertThat(cache).isNotNull();

        Cache.ValueWrapper cachedValue = cache.get("url:" + shortUrl.getShortCode());
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isEqualTo(shortUrl.getOriginalUrl());
    }

    @Test
    void getOriginalUrl_whenCacheHits_returnsValueFromRedis() {
        ShortUrl shortUrl = MotherObject.validShortUrlForIntegrationTest();
        urlRepository.save(shortUrl);

        Cache cache = cacheManager.getCache("shortUrls");
        assertThat(cache).isNotNull();

        cache.put("url:" + shortUrl.getShortCode(), shortUrl.getOriginalUrl());

        urlRepository.deleteAll();

        Optional<String> originalUrl = cachedShortUrlService.getOriginalUrl(shortUrl.getShortCode());
        assertThat(originalUrl)
                .isEqualTo(shortUrl.getOriginalUrl());
    }

    @Test
    void getOriginalUrl_whenCacheMisses_storesExpectedKeyAndValueInRedis() {
        ShortUrl shortUrl = MotherObject.validShortUrlForIntegrationTest();
        urlRepository.save(shortUrl);

        Optional<String> originalUrl = cachedShortUrlService.getOriginalUrl(shortUrl.getShortCode());

        String expectedKey = "url:" + shortUrl.getShortCode();
        String cachedValue = stringRedisTemplate.opsForValue().get(expectedKey);

        assertThat(originalUrl.get()).isEqualTo(shortUrl.getOriginalUrl());
        assertThat(cachedValue).isEqualTo(shortUrl.getOriginalUrl());
    }
}
