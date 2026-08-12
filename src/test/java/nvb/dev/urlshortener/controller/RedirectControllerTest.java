package nvb.dev.urlshortener.controller;

import nvb.dev.urlshortener.exception.InvalidShortCodeException;
import nvb.dev.urlshortener.exception.ShortUrlNotFoundException;
import nvb.dev.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @Test
    void redirect_whenShortCodeExists_redirectsToOriginalUrl() throws Exception {
        when(urlService.resolveShortCode(anyString())).thenReturn("https://google.com");

        mockMvc.perform(get("/{shortCode}", "dummy"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://google.com"));
    }

    @Test
    void redirect_whenShortCodeIsTooShort_returnsBadRequest() throws Exception {
        when(urlService.resolveShortCode(anyString()))
                .thenThrow(new InvalidShortCodeException("Short code is too short."));
        mockMvc.perform(get("/{shortCode}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Short code is too short."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void redirect_whenShortCodeIsTooLong_returnsBadRequest() throws Exception {
        when(urlService.resolveShortCode(anyString())).thenThrow(new InvalidShortCodeException("Short code is too long."));
        mockMvc.perform(get("/{shortCode}", "abcdefg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Short code is too long."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void redirect_whenShortCodeContainsInvalidCharacter_returnsBadRequest() throws Exception {
        when(urlService.resolveShortCode(anyString()))
                .thenThrow(new InvalidShortCodeException("Short code contains an invalid character."));
        mockMvc.perform(get("/{shortCode}", "a@bcde"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Short code contains an invalid character."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void redirect_whenServiceThrowsShortUrlNotFoundException_returnsNotFound() throws Exception {
        when(urlService.resolveShortCode(anyString()))
                .thenThrow(new ShortUrlNotFoundException("Short Url does not exist."));

        mockMvc.perform(get("/{shortCode}", "dummy"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Short Url does not exist."))
                .andExpect(jsonPath("$.statusCode").value(404));
    }
}