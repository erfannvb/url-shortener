package nvb.dev.urlshortener.util;

import lombok.RequiredArgsConstructor;
import nvb.dev.urlshortener.repository.UrlRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ExpiredShortUrlCleanup {

    private final UrlRepository urlRepository;

    @Scheduled(
            fixedDelayString = "${url.short.code.cleanup.interval.hours}",
            timeUnit = TimeUnit.HOURS
    )
    public void cleanUp() {
        urlRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());
    }

}
