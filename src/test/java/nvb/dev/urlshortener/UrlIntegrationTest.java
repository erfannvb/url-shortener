package nvb.dev.urlshortener;

import nvb.dev.urlshortener.domain.ShortUrl;
import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.dto.CreateUrlResponse;
import nvb.dev.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class UrlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UrlRepository urlRepository;

    @BeforeEach
    void cleanDatabase() {
        urlRepository.deleteAll();
    }

    @Test
    void createUrl_whenValidUrl_persistsUrlAndReturnsCreated() throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUrlRequest("https://youtube.com")))
                )
                .andExpect(status().isCreated())
                .andReturn();

        ShortUrl shortUrl = urlRepository.findByOriginalUrl("https://youtube.com").orElseThrow();

        String expectedLocation = "http://localhost/" + shortUrl.getShortCode();
        assertThat(mvcResult.getResponse().getHeader("Location")).isEqualTo(expectedLocation);

        CreateUrlResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), CreateUrlResponse.class);
        assertThat(response.shortenedUrl()).isEqualTo(expectedLocation);

        assertThat(shortUrl.getOriginalUrl()).isEqualTo("https://youtube.com");
        assertThat(shortUrl.getShortCode()).isNotBlank();
        assertThat(shortUrl.getShortCode()).hasSize(6);
    }

    @Test
    void redirect_whenShortUrlExists_redirectsToOriginalUrl() throws Exception {
        ShortUrl shortUrl = MotherObject.validShortUrlForIntegrationTest();
        urlRepository.save(shortUrl);

        mockMvc.perform(get("/{shortCode}", shortUrl.getShortCode()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", shortUrl.getOriginalUrl()));
    }

    @Test
    void redirect_whenShortUrlDoesNotExist_returnsNotFound() throws Exception {
        mockMvc.perform(get("/{shortCode}", "abc123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Short Url does not exist."))
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void createUrl_whenUrlAlreadyExists_returnsExistingShortCode() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUrlRequest("https://google.com")))
                )
                .andExpect(status().isCreated());

        ShortUrl firstShortUrl = urlRepository
                .findByOriginalUrl("https://google.com")
                .orElseThrow();

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUrlRequest("https://google.com")))
                )
                .andExpect(status().isCreated());

        ShortUrl secondShortUrl = urlRepository
                .findByOriginalUrl("https://google.com")
                .orElseThrow();

        List<ShortUrl> urls = urlRepository.findAllByOriginalUrl("https://google.com");

        assertThat(secondShortUrl.getShortCode()).isEqualTo(firstShortUrl.getShortCode());
        assertThat(urls).hasSize(1);
    }

    @Test
    void createUrl_whenUrlIsInvalid_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUrlRequest("google.com")))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Url must start with http:// or https://"))
                .andExpect(jsonPath("$.statusCode").value(400));

        Optional<ShortUrl> foundShortUrl = urlRepository.findByOriginalUrl("google.com");
        assertThat(foundShortUrl).isEmpty();
    }

    @Test
    void redirect_whenShortCodeIsTooShort_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/{shortCode}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Short code is too short."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void redirect_whenShortCodeIsTooLong_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/{shortCode}", "abcdefg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Short code is too long."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void redirect_whenShortCodeContainsInvalidCharacter_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/{shortCode}", "abc@12"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Short code contains an invalid character."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createUrl_whenShortCodeIsAlreadyUsed_generatesDifferentShortCode() throws Exception {
        ShortUrl shortUrl = MotherObject.validShortUrlWithExistingShortUrl();
        urlRepository.save(shortUrl);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUrlRequest("https://youtube.com")))
                )
                .andExpect(status().isCreated());

        Optional<ShortUrl> newShortUrl = urlRepository.findByOriginalUrl("https://youtube.com");

        assertThat(newShortUrl).isPresent();
        assertThat(newShortUrl.get().getShortCode()).isNotEqualTo("abc123");
    }

    @Test
    void createUrl_whenUrlIsMissing_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUrlRequest(null)))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("url cannot be blank."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createUrl_whenUrlIsBlank_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUrlRequest("")))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("url cannot be blank."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void createUrl_whenUrlContainsWhiteSpacesOnly_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateUrlRequest("   ")))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("url cannot be blank."))
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    void saveShortUrl_whenShortCodeAlreadyExists_throwsDataIntegrityViolationException() {
        ShortUrl shortUrl = MotherObject.validShortUrlForIntegrationTest();
        urlRepository.save(shortUrl);

        ShortUrl duplicate = MotherObject.anyValidDupliateShortUrl();

        assertThatThrownBy(() -> urlRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteAllByExpiresAtBefore_deletesExpiredUrlsAndKeepsValidUrls() {
        ShortUrl expiredShortUrl = MotherObject.anyExpiredShortUrl();
        ShortUrl validShortUrl = MotherObject.anyNewValidShortUrl();

        urlRepository.saveAll(List.of(expiredShortUrl, validShortUrl));
        urlRepository.deleteAllByExpiresAtBefore(LocalDateTime.now());

        assertThat(urlRepository.existsById(expiredShortUrl.getId())).isFalse();
        assertThat(urlRepository.existsById(validShortUrl.getId())).isTrue();
    }
}
