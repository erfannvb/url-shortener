package nvb.dev.urlshortener;

import nvb.dev.urlshortener.domain.ShortUrl;

import java.time.LocalDateTime;
import java.util.UUID;

public class MotherObject {

    public static final String ANY_STRING = "dummy";
    public static final String ALLOWED_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final long SHORT_URL_EXPIRATION_DAYS = 15;

    public static ShortUrl anyValidShortUrl() {
        return new ShortUrl(
                UUID.randomUUID(),
                ANY_STRING,
                ANY_STRING,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(SHORT_URL_EXPIRATION_DAYS)
        );
    }

    public static ShortUrl anyValidShortUrl2() {
        return new ShortUrl(
                UUID.randomUUID(),
                ANY_STRING,
                ANY_STRING,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        );
    }

    public static ShortUrl validShortUrlForIntegrationTest() {
        return new ShortUrl(
                null,
                "https://youtube.com",
                "abc123",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(SHORT_URL_EXPIRATION_DAYS)
        );
    }

    public static ShortUrl validShortUrlWithExistingShortUrl() {
        return new ShortUrl(
                null,
                "https://existing.com",
                "abc123",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(SHORT_URL_EXPIRATION_DAYS)
        );
    }

    public static ShortUrl anyValidDupliateShortUrl() {
        return new ShortUrl(
                null,
                "https://example.com",
                "abc123",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(SHORT_URL_EXPIRATION_DAYS)
        );
    }

    public static ShortUrl anyExpiredShortUrl() {
        return new ShortUrl(
                null,
                "https://example.com",
                "abc123",
                LocalDateTime.now(),
                LocalDateTime.now().minusDays(SHORT_URL_EXPIRATION_DAYS)
        );
    }
}
