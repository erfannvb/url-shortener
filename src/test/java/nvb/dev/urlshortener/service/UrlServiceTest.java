package nvb.dev.urlshortener.service;

import nvb.dev.urlshortener.MotherObject;
import nvb.dev.urlshortener.domain.ShortUrl;
import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.dto.CreateUrlResponse;
import nvb.dev.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    @Captor
    ArgumentCaptor<ShortUrl> shortUrlArgumentCaptor;

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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Url must start with http:// or https://");

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
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString()))
                .thenReturn(Optional.of(MotherObject.anyValidShortUrl()))
                .thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenReturn(MotherObject.anyValidShortUrl());

        CreateUrlResponse response = urlService.shortenUrl(new CreateUrlRequest("https://google.com"));

        assertThat(response).isNotNull();

        verify(urlRepository, times(2)).findByShortCode(anyString());
        verify(urlRepository, times(1)).save(any(ShortUrl.class));
    }

    @Test
    void shortenUrl_whenUniqueShortCodeCannotBeGenerated_throwsException() {
        when(urlRepository.findByOriginalUrl(anyString())).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.of(MotherObject.anyValidShortUrl()));

        assertThatThrownBy(() -> urlService.shortenUrl(new CreateUrlRequest("https://google.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Short code could not be generated.");

        verify(urlRepository, times(10)).findByShortCode(anyString());
        verify(urlRepository, never()).save(any(ShortUrl.class));
    }

    @Test
    void resolveShortCode_whenShortCodeExists_returnsShortUrl() {
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.of(MotherObject.anyValidShortUrl()));
        ShortUrl shortUrl = urlService.resolveShortCode("dummy");
        assertAll(
                () -> assertThat(shortUrl).isNotNull(),
                () -> {
                    assert shortUrl != null;
                    assertThat(shortUrl.getShortCode()).isEqualTo("dummy");
                }
        );
        verify(urlRepository).findByShortCode(anyString());
    }

    @Test
    void resolveShortCode_whenShortCodeDoesNotExist_throwsException() {
        when(urlRepository.findByShortCode(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.resolveShortCode("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Short Url does not exist.");

        verify(urlRepository, times(1)).findByShortCode(anyString());
    }

    @Test
    void shortenUrl_whenUrlDoesNotExist_savesCorrectShortUrl() {
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
}