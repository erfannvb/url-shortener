package nvb.dev.urlshortener.controller;

import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlService urlService;

    @Test
    void shortenUrl_whenServiceThrowsIllegalArgumentException_returnsBadRequest() throws Exception {
        when(urlService.shortenUrl(any(CreateUrlRequest.class)))
                .thenThrow(new IllegalArgumentException("Url must start with http:// or https://"));

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUrlRequest("amazon.com")))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Url must start with http:// or https://"))
                .andExpect(jsonPath("$.statusCode").value(400));
    }
}