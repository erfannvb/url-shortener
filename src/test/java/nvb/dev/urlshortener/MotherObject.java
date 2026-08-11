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

}
