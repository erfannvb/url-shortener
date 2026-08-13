package nvb.dev.urlshortener;

import nvb.dev.urlshortener.domain.ShortUrl;

import java.time.LocalDateTime;
import java.util.UUID;

public class MotherObject {

    public static final String ANY_STRING = "dummy";

    public static ShortUrl anyValidShortUrl() {
        return new ShortUrl(
                UUID.randomUUID(),
                ANY_STRING,
                ANY_STRING,
                LocalDateTime.now()
        );
    }

    public static ShortUrl validShortUrlForIntegrationTest() {
        return new ShortUrl(
                null,
                "https://youtube.com",
                "abc123",
                LocalDateTime.now()
        );
    }

    public static ShortUrl validShortUrlWithExistingShortUrl() {
        return new ShortUrl(
                null,
                "https://existing.com",
                "abc123",
                LocalDateTime.now()
        );
    }

    public static ShortUrl anyValidDupliateShortUrl() {
        return new ShortUrl(
                null,
                "https://example.com",
                "abc123",
                LocalDateTime.now()
        );
    }

}
