package nvb.dev.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachedShortUrlServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CachedShortUrlService cachedShortUrlService;

    @Captor
    ArgumentCaptor<Duration> ttlCaptor;

    @BeforeEach
    void setUp() {
        cachedShortUrlService = new CachedShortUrlService(stringRedisTemplate);
    }

    @Test
    void getOriginalUrl_whenCacheHit_returnsCachedUrl() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:abc123")).thenReturn("https://example.com");

        Optional<String> result = cachedShortUrlService.getOriginalUrl("abc123");

        assertThat(result)
                .isPresent()
                .contains("https://example.com");

        verify(valueOperations).get("url:abc123");
    }

    @Test
    void getOriginalUrl_whenCacheMiss_returnsEmptyOptional() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:abc123")).thenReturn(null);

        Optional<String> result = cachedShortUrlService.getOriginalUrl("abc123");

        assertThat(result).isEmpty();
        verify(valueOperations).get("url:abc123");
    }

    @Test
    void getOriginalUrl_whenRedisFails_returnsEmptyOptional() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:abc123")).thenThrow(new RedisConnectionFailureException("Redis unavailable"));

        Optional<String> result = cachedShortUrlService.getOriginalUrl("abc123");

        assertThat(result).isEmpty();
        verify(valueOperations).get("url:abc123");
    }

    @Test
    void cacheOriginalUrl_whenUrlIsNotExpired_storesUrlWithRemainingTtl() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        cachedShortUrlService.cacheOriginalUrl("abc123", "https://example.com", expiresAt);

        verify(valueOperations).set(eq("url:abc123"), eq("https://example.com"), ttlCaptor.capture());
        Duration ttl = ttlCaptor.getValue();
        assertThat(ttl)
                .isPositive()
                .isLessThanOrEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void cacheOriginalUrl_whenUrlIsExpired_doesNotWriteToRedis() {
        LocalDateTime expiresAt = LocalDateTime.now().minusMinutes(1);

        cachedShortUrlService.cacheOriginalUrl(
                "abc123",
                "https://example.com",
                expiresAt
        );

        verifyNoInteractions(valueOperations);
    }

    @Test
    void cacheOriginalUrl_whenRedisFails_doesNotPropagateException() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        doThrow(new RedisConnectionFailureException("Redis unavailable"))
                .when(valueOperations)
                .set(
                        eq("url:abc123"),
                        eq("https://example.com"),
                        any(Duration.class)
                );

        assertThatCode(() ->
                cachedShortUrlService.cacheOriginalUrl(
                        "abc123",
                        "https://example.com",
                        expiresAt
                )
        ).doesNotThrowAnyException();

        verify(valueOperations).set(
                eq("url:abc123"),
                eq("https://example.com"),
                any(Duration.class)
        );
    }
}