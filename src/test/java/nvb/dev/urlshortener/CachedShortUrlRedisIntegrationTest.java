package nvb.dev.urlshortener;

import nvb.dev.urlshortener.service.CachedShortUrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;
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
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.delete("url:abc123");
    }

    @Test
    void getOriginalUrl_whenCacheMisses_returnsEmpty() {
        Optional<String> originalUrl = cachedShortUrlService.getOriginalUrl("abc123");
        assertThat(originalUrl).isEmpty();
    }

    @Test
    void getOriginalUrl_whenCacheHits_returnsValueFromRedis() {
        stringRedisTemplate.opsForValue().set("url:abc123", "https://example.com", Duration.ofMinutes(1));

        Optional<String> originalUrl = cachedShortUrlService.getOriginalUrl("abc123");
        assertThat(originalUrl).contains("https://example.com");
    }

    @Test
    void cacheOriginalUrl_storesExpectedKeyAndValueInRedis() {
        cachedShortUrlService.cacheOriginalUrl("abc123", "https://example.com", LocalDateTime.now().plusMinutes(4));

        String originalUrl = stringRedisTemplate.opsForValue().get("url:abc123");
        assertThat(originalUrl).isEqualTo("https://example.com");
    }

    @Test
    void cacheOriginalUrl_setsExpectedTtl() {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
        cachedShortUrlService.cacheOriginalUrl("abc123", "https://example.com", expiresAt);

        Long expireValue = stringRedisTemplate.getExpire("url:abc123");

        assertThat(expireValue).isPositive();
        assertThat(expireValue).isBetween(590L, 600L);
    }

    @Test
    void cacheOriginalUrl_whenAlreadyExpired_doesNotCache() {
        LocalDateTime expiresAt = LocalDateTime.now().minusDays(10);
        cachedShortUrlService.cacheOriginalUrl("abc123", "https://example.com", expiresAt);

        assertThat(stringRedisTemplate.hasKey("url:abc123")).isFalse();
    }
}
