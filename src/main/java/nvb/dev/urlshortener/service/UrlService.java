package nvb.dev.urlshortener.service;

import lombok.RequiredArgsConstructor;
import nvb.dev.urlshortener.domain.ShortUrl;
import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.dto.CreateUrlResponse;
import nvb.dev.urlshortener.exception.InvalidShortCodeException;
import nvb.dev.urlshortener.exception.InvalidUrlException;
import nvb.dev.urlshortener.exception.ShortCodeGenerationException;
import nvb.dev.urlshortener.exception.ShortUrlNotFoundException;
import nvb.dev.urlshortener.repository.UrlRepository;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UrlService {

    private static final SecureRandom RANDOM = new SecureRandom();
    @Value("${url.short.code.length}")
    private int shortCodeLength;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private static final String CHARACTER_SET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});

    private final UrlRepository urlRepository;

    @Transactional
    public CreateUrlResponse shortenUrl(CreateUrlRequest request) {
        String url = request.url();

        if (!URL_VALIDATOR.isValid(url))
            throw new InvalidUrlException("Invalid URL.");

        String normalizedUrl = normalizeUrl(url);

        Optional<ShortUrl> existingShortUrl = urlRepository.findByOriginalUrl(normalizedUrl);
        if (existingShortUrl.isPresent()) {
            return mapShortUrlToResponse(existingShortUrl.get());
        }

        ShortUrl shortUrl = generateUniqueShortCode(normalizedUrl);
        return mapShortUrlToResponse(shortUrl);
    }

    public String resolveShortCode(String shortCode) {
        int length = shortCode.length();
        if (length < shortCodeLength) throw new InvalidShortCodeException("Short code is too short.");
        if (length > shortCodeLength) throw new InvalidShortCodeException("Short code is too long.");
        if (!containsOnlyAllowedCharacters(shortCode))
            throw new InvalidShortCodeException("Short code contains an invalid character.");

        return urlRepository.findByShortCode(shortCode)
                .map(ShortUrl::getOriginalUrl)
                .orElseThrow(() -> new ShortUrlNotFoundException("Short Url does not exist."));
    }

    private String generateShortCode() {
        StringBuilder result = new StringBuilder(shortCodeLength);

        for (int i = 0; i < shortCodeLength; i++) {
            int index = RANDOM.nextInt(CHARACTER_SET.length());
            result.append(CHARACTER_SET.charAt(index));
        }

        return result.toString();
    }

    private ShortUrl generateUniqueShortCode(String originalUrl) {
        String shortCode;

        for (int i = 0; i < MAX_GENERATION_ATTEMPTS; i++) {
            shortCode = generateShortCode();
            Optional<ShortUrl> existingShortCode = urlRepository.findByShortCode(shortCode);
            if (existingShortCode.isPresent())
                continue;

            ShortUrl shortUrl = ShortUrl.builder()
                    .originalUrl(originalUrl)
                    .shortCode(shortCode)
                    .build();

            try {
                return urlRepository.save(shortUrl);
            } catch (DataIntegrityViolationException e) {
                continue;
            }

        }

        throw new ShortCodeGenerationException("Short code could not be generated.");
    }

    private boolean containsOnlyAllowedCharacters(String shortCode) {
        for (char ch : shortCode.toCharArray()) {
            if (CHARACTER_SET.indexOf(ch) < 0) {
                return false;
            }
        }
        return true;
    }

    private String normalizeUrl(final String originalUrl) {
        try {
            URI uri = new URI(originalUrl);
            URI normalizedUri = new URI(
                    uri.getScheme().toLowerCase(),
                    uri.getUserInfo(),
                    uri.getHost() != null ? uri.getHost().toLowerCase() : null,
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()
            );
            return normalizedUri.toString();
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Invalid URL.");
        }
    }

    private CreateUrlResponse mapShortUrlToResponse(ShortUrl shortUrl) {
        return new CreateUrlResponse(shortUrl.getShortCode());
    }

}
