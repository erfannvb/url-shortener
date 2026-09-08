package nvb.dev.urlshortener.service;

import nvb.dev.urlshortener.MotherObject;
import nvb.dev.urlshortener.domain.ShortUrl;
import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.dto.CreateUrlResponse;
import nvb.dev.urlshortener.exception.InvalidShortCodeException;
import nvb.dev.urlshortener.exception.InvalidUrlException;
import nvb.dev.urlshortener.exception.ShortCodeGenerationException;
import nvb.dev.urlshortener.exception.ShortUrlNotFoundException;
import nvb.dev.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private CachedShortUrlService cachedShortUrlService;

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    @Captor
    ArgumentCaptor<ShortUrl> shortUrlArgumentCaptor;

    @Captor
    ArgumentCaptor<String> originalUrlArgumentCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "characterSet", MotherObject.ALLOWED_CHARS);
    }

    @Test
    void shortenUrl_whenUrlDoesNotExist_createsAndReturnsShortUrl() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://google.com"));

        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> {
                    assert response != null;
                    assertThat(response.shortenedUrl()).isEqualTo("dummy");
                }
        );

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).save(any(ShortUrl.class));
    }

    @Test
    void shortenUrl_whenUrlDoesNotStartWithHttpOrHttps_rejectsTheUrl() {
        assertThatThrownBy(() -> urlService.shortenUrl(new CreateUrlRequest("dummy.com")))
                .isInstanceOf(InvalidUrlException.class)
                .hasMessage("Invalid URL.");

        verifyNoInteractions(urlRepository);
    }

    @Test
    void shortenUrl_whenUrlAlreadyExists_returnsExistingShortCode() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.of(MotherObject.anyValidShortUrl()));
        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://google.com"));

        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> {
                    assert response != null;
                    assertThat(response.shortenedUrl()).isEqualTo("dummy");
                }
        );

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository, never()).save(any(ShortUrl.class));
    }

    @Test
    void shortenUrl_whenGeneratedShortCodeCollides_retriesWithAnotherCode() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString()))
                .thenReturn(Optional.of(MotherObject.anyValidShortUrl()))
                .thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://google.com"));

        assertThat(response).isNotNull();

        verify(urlRepository, times(2)).findByShortCode(anyString());
        verify(urlRepository, times(1)).save(shortUrlArgumentCaptor.capture());

        ShortUrl shortUrl = shortUrlArgumentCaptor.getValue();
        assertThat(shortUrl).isNotNull();
        assertThat(shortUrl.getOriginalUrl()).isEqualTo("https://google.com");
        assertThat(shortUrl.getShortCode())
                .isNotBlank()
                .hasSize(6);
    }

    @Test
    void shortenUrl_whenUniqueShortCodeCannotBeGenerated_throwsException() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.of(MotherObject.anyValidShortUrl()));

        assertThatThrownBy(() -> urlService.shortenUrl(new CreateUrlRequest("https://google.com")))
                .isInstanceOf(ShortCodeGenerationException.class)
                .hasMessage("Short code could not be generated.");

        verify(urlRepository, times(10)).findByShortCode(anyString());
        verify(urlRepository, never()).save(any(ShortUrl.class));
    }

    @Test
    void resolveShortCode_whenShortCodeIsTooShort_throwsException() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        assertThatThrownBy(() -> urlService.resolveShortCode("abc"))
                .isInstanceOf(InvalidShortCodeException.class)
                .hasMessage("Short code is too short.");

        verifyNoInteractions(urlRepository);
        verifyNoInteractions(cachedShortUrlService);
    }

    @Test
    void resolveShortCode_whenShortCodeIsTooLong_throwsException() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        assertThatThrownBy(() -> urlService.resolveShortCode("abcdefg"))
                .isInstanceOf(InvalidShortCodeException.class)
                .hasMessage("Short code is too long.");

        verifyNoInteractions(urlRepository);
        verifyNoInteractions(cachedShortUrlService);
    }

    @Test
    void resolveShortCode_whenShortCodeContainsInvalidCharacter_throwsException() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        assertThatThrownBy(() -> urlService.resolveShortCode("ab@!df"))
                .isInstanceOf(InvalidShortCodeException.class)
                .hasMessage("Short code contains an invalid character.");

        verifyNoInteractions(urlRepository);
        verifyNoInteractions(cachedShortUrlService);
    }

    @Test
    void resolveShortCode_whenShortCodeDoesNotExist_throwsException() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.resolveShortCode("abcdef"))
                .isInstanceOf(ShortUrlNotFoundException.class)
                .hasMessage("Short Url does not exist.");

        verify(urlRepository, times(1)).findByShortCode(anyString());
    }

    @Test
    void shortenUrl_whenUrlDoesNotExist_savesCorrectShortUrl() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://google.com"));

        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> {
                    assert response != null;
                    assertThat(response.shortenedUrl()).isEqualTo("dummy");
                }
        );

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).save(shortUrlArgumentCaptor.capture());

        ShortUrl value = shortUrlArgumentCaptor.getValue();
        assertAll(
                () -> assertThat(value.getOriginalUrl()).isEqualTo("https://google.com"),
                () -> assertThat(value.getShortCode()).isNotNull(),
                () -> {
                    assert value.getShortCode() != null;
                    assertThat(value.getShortCode().length()).isEqualTo(6);
                }
        );
    }

    @Test
    void shortenUrl_whenSavingShortCodeCausesConstraintViolation_retriesWithAnotherCode() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException(""))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://google.com"));

        assertThat(response).isNotNull();
        assertThat(response.shortenedUrl()).isNotBlank();
        assertThat(response.shortenedUrl()).hasSize(6);

        verify(urlRepository, times(2)).findByShortCode(anyString());
        verify(urlRepository, times(2)).save(any(ShortUrl.class));
    }

    @Test
    void shortenUrl_whenAllShortCodeGenerationAttemptsFail_throwsException() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenThrow(new DataIntegrityViolationException(""));

        assertThatThrownBy(() -> urlService.shortenUrl(new CreateUrlRequest("https://google.com")))
                .isInstanceOf(ShortCodeGenerationException.class)
                .hasMessage("Short code could not be generated.");

        verify(urlRepository, times(10)).findByShortCode(anyString());
        verify(urlRepository, times(10)).save(any(ShortUrl.class));
    }

    @Test
    void shortenUrl_whenSchemeIsUppercase_normalizesSchemeToLowercase() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("HTTPS://google.com"));

        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> {
                    assert response != null;
                    assertThat(response.shortenedUrl()).isNotBlank();
                }
        );

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).save(shortUrlArgumentCaptor.capture());

        String originalUrl = shortUrlArgumentCaptor.getValue().getOriginalUrl();
        assertThat(originalUrl).isEqualTo("https://google.com");
    }

    @Test
    void shortenUrl_whenHostnameContainsUppercase_normalizesHostnameToLowercase() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://GOoGlE.com"));

        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> {
                    assert response != null;
                    assertThat(response.shortenedUrl()).isNotBlank();
                }
        );

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).save(shortUrlArgumentCaptor.capture());

        String originalUrl = shortUrlArgumentCaptor.getValue().getOriginalUrl();
        assertThat(originalUrl).isEqualTo("https://google.com");
    }

    @Test
    void shortenUrl_whenPathContainsUppercase_preservesPathCase() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(
                new CreateUrlRequest("https://google.com/PATH")
        );

        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> {
                    assert response != null;
                    assertThat(response.shortenedUrl()).isNotBlank();
                }
        );

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).save(shortUrlArgumentCaptor.capture());

        String originalUrl = shortUrlArgumentCaptor.getValue().getOriginalUrl();
        assertThat(originalUrl).isEqualTo("https://google.com/PATH");
    }

    @Test
    void shortenUrl_whenQueryContainsUppercase_preservesQueryCase() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(
                new CreateUrlRequest("https://google.com/PATH?Name=string")
        );

        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> {
                    assert response != null;
                    assertThat(response.shortenedUrl()).isNotBlank();
                }
        );

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).save(shortUrlArgumentCaptor.capture());

        String originalUrl = shortUrlArgumentCaptor.getValue().getOriginalUrl();
        assertThat(originalUrl).isEqualTo("https://google.com/PATH?Name=string");
    }

    @Test
    void shortenUrl_whenFragmentContainsUppercase_preservesFragmentCase() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(
                new CreateUrlRequest("https://google.com/PATH#Fragment=1")
        );

        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> {
                    assert response != null;
                    assertThat(response.shortenedUrl()).isNotBlank();
                }
        );

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).save(shortUrlArgumentCaptor.capture());

        String originalUrl = shortUrlArgumentCaptor.getValue().getOriginalUrl();
        assertThat(originalUrl).isEqualTo("https://google.com/PATH#Fragment=1");
    }

    @Test
    void shortenUrl_whenNormalizedUrlAlreadyExists_returnsExistingShortCode() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.of(MotherObject.anyValidShortUrl()));

        CreateUrlResponse response = urlService.shortenUrl(
                new CreateUrlRequest("HTTPS://Example.COM")
        );

        assertAll(
                () -> assertThat(response).isNotNull(),
                () -> {
                    assert response != null;
                    assertThat(response.shortenedUrl()).isNotBlank();
                }
        );

        verify(urlRepository).findByOriginalUrl(originalUrlArgumentCaptor.capture());

        String originalUrl = originalUrlArgumentCaptor.getValue();
        assertThat(originalUrl).isEqualTo("https://example.com");

        verify(urlRepository, never()).save(any(ShortUrl.class));
    }

    @Test
    void shortenUrl_whenGeneratingShortCode_generatesCodeWithValidLengthAndCharacters() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://google.com"));

        assertThat(response).isNotNull();

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).save(shortUrlArgumentCaptor.capture());

        String shortCode = shortUrlArgumentCaptor.getValue().getShortCode();
        assertThat(shortCode).hasSize(6);
        assertThat(shortCode.chars()).allMatch(ch -> MotherObject.ALLOWED_CHARS.indexOf(ch) >= 0);
    }

    @Test
    void resolveShortCode_whenShortCodeContainsConfiguredCharacter_acceptsIt() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 3);
        ReflectionTestUtils.setField(urlService, "characterSet", "ab1");

        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.of(MotherObject.anyValidShortUrl()));

        String shortCode = urlService.resolveShortCode("b1a");

        assertThat(shortCode).isNotBlank();
        assertThat(shortCode).isEqualTo("dummy");

        verify(urlRepository).findByShortCode("b1a");
    }

    @Test
    void resolveShortCode_whenShortCodeContainsUnconfiguredCharacter_rejectsIt() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 3);
        ReflectionTestUtils.setField(urlService, "characterSet", "abc123");

        assertThatThrownBy(() -> urlService.resolveShortCode("a1x"))
                .isInstanceOf(InvalidShortCodeException.class)
                .hasMessage("Short code contains an invalid character.");

        verifyNoInteractions(urlRepository);
    }

    @Test
    void shortenUrl_whenCreatingShortUrl_setsExpirationDate() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);
        ReflectionTestUtils.setField(urlService, "shortCodeExpirationDays", MotherObject.SHORT_URL_EXPIRATION_DAYS);

        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://example.com"));

        assertThat(response).isNotNull();
        assertThat(response.shortenedUrl()).isNotBlank();

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).save(shortUrlArgumentCaptor.capture());

        ShortUrl shortUrl = shortUrlArgumentCaptor.getValue();
        assertThat(shortUrl.getExpiresAt()).isNotNull();
        assertThat(shortUrl.getExpiresAt()).isCloseTo(
                LocalDateTime.now().plusDays(MotherObject.SHORT_URL_EXPIRATION_DAYS),
                within(1, ChronoUnit.SECONDS)
        );
    }

    @Test
    void shortenUrl_whenExistingUrlHasNotExpired_returnsExistingShortCode() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.of(MotherObject.anyValidShortUrl2()));
        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://example.com"));

        assertThat(response).isNotNull();
        assertThat(response.shortenedUrl()).isEqualTo("dummy");

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository, never()).save(any(ShortUrl.class));
        verify(urlRepository, never()).deleteById(any());
    }

    @Test
    void shortenUrl_whenExistingUrlHasExpired_deletesAndCreatesNewShortUrl() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);
        ReflectionTestUtils.setField(urlService, "shortCodeExpirationDays", MotherObject.SHORT_URL_EXPIRATION_DAYS);

        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.of(MotherObject.anyExpiredShortUrl()));
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://example.com"));

        assertThat(response).isNotNull();
        assertThat(response.shortenedUrl()).isNotBlank();

        verify(urlRepository).findByOriginalUrl(anyString());
        verify(urlRepository).findByShortCode(anyString());

        verify(urlRepository).save(shortUrlArgumentCaptor.capture());
        ShortUrl shortUrl = shortUrlArgumentCaptor.getValue();
        assertThat(shortUrl.getOriginalUrl()).isEqualTo("https://example.com");
        assertThat(shortUrl.getShortCode()).isNotBlank();
        assertThat(shortUrl.getShortCode()).hasSize(6);
        assertThat(shortUrl.getExpiresAt()).isCloseTo(
                LocalDateTime.now().plusDays(MotherObject.SHORT_URL_EXPIRATION_DAYS),
                within(1, ChronoUnit.SECONDS)
        );

        verify(urlRepository).deleteById(any());
    }

    @Test
    void resolveShortCode_whenShortCodeIsExpired_throwsException() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(cachedShortUrlService.getOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.of(MotherObject.anyExpiredShortUrl()));

        assertThatThrownBy(() -> urlService.resolveShortCode("abc123"))
                .isInstanceOf(ShortUrlNotFoundException.class)
                .hasMessage("Short Url does not exist.");

        verify(urlRepository, times(1)).findByShortCode(anyString());
        verify(cachedShortUrlService, never()).cacheOriginalUrl(anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void shortenUrl_whenCalledConcurrently_generatesUniqueShortCodes() throws InterruptedException, ExecutionException {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        try (ExecutorService executor = Executors.newFixedThreadPool(20)) {

            CountDownLatch ready = new CountDownLatch(20);
            CountDownLatch start = new CountDownLatch(1);

            List<Callable<CreateUrlResponse>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String url = "http://example" + i + ".com";
                tasks.add(() -> {
                    ready.countDown();
                    start.await();
                    return urlService.shortenUrl(new CreateUrlRequest(url));
                });
            }

            List<Future<CreateUrlResponse>> futures = tasks.stream()
                    .map(executor::submit)
                    .toList();

            ready.await();
            start.countDown();

            List<String> shortCodes = new ArrayList<>();

            for (Future<CreateUrlResponse> future : futures) {
                CreateUrlResponse response = future.get();
                shortCodes.add(response.shortenedUrl());
            }

            assertThat(shortCodes).hasSize(20)
                    .allMatch(code -> code.length() == 6)
                    .doesNotHaveDuplicates();

            verify(urlRepository, times(20)).save(shortUrlArgumentCaptor.capture());
            List<String> savedShortCodes = shortUrlArgumentCaptor.getAllValues()
                    .stream()
                    .map(ShortUrl::getShortCode)
                    .toList();

            assertThat(savedShortCodes)
                    .hasSize(20)
                    .doesNotHaveDuplicates();
        }
    }

    @Test
    void resolveShortCode_whenCacheHits_returnsCachedUrl() {
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(cachedShortUrlService.getOriginalUrl(anyString())).thenReturn(Optional.of("https://example.com"));

        String originalUrl = urlService.resolveShortCode("abc123");
        assertThat(originalUrl).isNotNull();
        assertThat(originalUrl).isEqualTo("https://example.com");

        verify(urlRepository, never()).findByShortCode(anyString());
    }

    @Test
    void resolveShortCode_whenCacheMissesAndUrlExists_returnsOriginalUrl() {
        ShortUrl shortUrl = MotherObject.anyValidShortUrl();
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(cachedShortUrlService.getOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.of(shortUrl));

        String originalUrl = urlService.resolveShortCode("abc123");
        assertThat(originalUrl).isNotBlank();
        assertThat(originalUrl).isEqualTo("dummy");

        verify(urlRepository).findByShortCode(anyString());
        verify(cachedShortUrlService).getOriginalUrl(anyString());
        verify(cachedShortUrlService).cacheOriginalUrl("abc123", shortUrl.getOriginalUrl(), shortUrl.getExpiresAt());
    }

    @Test
    void resolveShortCode_whenCacheFails_fallsBackToDatabase() {
        ShortUrl shortUrl = MotherObject.anyValidShortUrl();
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);

        when(cachedShortUrlService.getOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.of(shortUrl));

        String originalUrl = urlService.resolveShortCode("abc123");
        assertThat(originalUrl).isNotBlank();
        assertThat(originalUrl).isEqualTo(shortUrl.getOriginalUrl());

        verify(cachedShortUrlService).getOriginalUrl(anyString());
        verify(urlRepository).findByShortCode(anyString());
    }
}