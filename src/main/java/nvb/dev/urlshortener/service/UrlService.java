package nvb.dev.urlshortener.service;

import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.dto.CreateUrlResponse;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class UrlService {

    private static final Random RANDOM = new Random();
    private static final int SHORT_CODE_LENGTH = 6;

    private static final String CHARACTER_SET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public CreateUrlResponse shortenUrl(CreateUrlRequest request) {
        String url = request.url();

        if (!url.startsWith("http://") && !url.startsWith("https://"))
            throw new IllegalArgumentException();

        return new CreateUrlResponse(generateShortCode());
    }

    private String generateShortCode() {
        StringBuilder result = new StringBuilder(6);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CHARACTER_SET.length());
            result.append(CHARACTER_SET.charAt(index));
        }

        return result.toString();
    }

}
