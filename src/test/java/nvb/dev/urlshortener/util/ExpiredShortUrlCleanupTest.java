package nvb.dev.urlshortener.util;

import nvb.dev.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpiredShortUrlCleanupTest {

    @Mock
    private UrlRepository urlRepository;

    @Captor
    ArgumentCaptor<LocalDateTime> dateTimeCaptor;

    @InjectMocks
    private ExpiredShortUrlCleanup expiredShortUrlCleanup;

    @Test
    void cleanUp_deletesExpiredShortUrls() {
        expiredShortUrlCleanup.cleanUp();
        verify(urlRepository).deleteAllByExpiresAtBefore(dateTimeCaptor.capture());
        assertThat(dateTimeCaptor.getValue()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
    }
}