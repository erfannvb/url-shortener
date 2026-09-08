package nvb.dev.urlshortener.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CachedShortUrlService {

    private static final Logger log = LoggerFactory.getLogger(CachedShortUrlService.class);
    private static final String URL_KEY_PREFIX = "url:";

    private final StringRedisTemplate stringRedisTemplate;

    public Optional<String> getOriginalUrl(String shortCode) {
        try {
            String cacheUrl = stringRedisTemplate.opsForValue().get(URL_KEY_PREFIX + shortCode);
            return Optional.ofNullable(cacheUrl);
        } catch (RedisConnectionFailureException e) {
            return Optional.empty();
        }
    }

    public void cacheOriginalUrl(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        LocalDateTime now = LocalDateTime.now();

        if (!expiresAt.isAfter(now))
            return;

        Duration remainingTtl = Duration.between(now, expiresAt);
        try {
            stringRedisTemplate.opsForValue().set(URL_KEY_PREFIX + shortCode, originalUrl, remainingTtl);
        } catch (RedisConnectionFailureException e) {
            log.warn("{}", e.getMessage());
        }
    }

}
