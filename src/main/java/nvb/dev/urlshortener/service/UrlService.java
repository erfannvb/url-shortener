package nvb.dev.urlshortener.service;

import lombok.RequiredArgsConstructor;
import nvb.dev.urlshortener.domain.ShortUrl;
import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.dto.CreateUrlResponse;
import nvb.dev.urlshortener.repository.UrlRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UrlService {

    private static final Random RANDOM = new Random();
    private static final int SHORT_CODE_LENGTH = 6;

    private static final String CHARACTER_SET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final UrlRepository urlRepository;

    public CreateUrlResponse shortenUrl(CreateUrlRequest request) {
        String url = request.url();
        String normalizedUrl = url.toLowerCase();

        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://"))
            throw new IllegalArgumentException();

        Optional<ShortUrl> existingShortUrl = urlRepository.findByOriginalUrl(url);
        if (existingShortUrl.isPresent()) {
            return mapShortUrlToResponse(existingShortUrl.get());
        }

        String shortCode = generateShortCode();
        ShortUrl newShortUrl = ShortUrl.builder()
                .originalUrl(url)
                .shortCode(shortCode)
                .build();

        ShortUrl savedShortUrl = urlRepository.save(newShortUrl);
        return mapShortUrlToResponse(savedShortUrl);
    }

    private String generateShortCode() {
        StringBuilder result = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTER_SET.length());
            result.append(CHARACTER_SET.charAt(index));
        }

        return result.toString();
    }

    private CreateUrlResponse mapShortUrlToResponse(ShortUrl shortUrl) {
        return new CreateUrlResponse(shortUrl.getShortCode());
    }

}
