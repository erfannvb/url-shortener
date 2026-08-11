package nvb.dev.urlshortener.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

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