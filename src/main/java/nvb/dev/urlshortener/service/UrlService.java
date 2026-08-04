package nvb.dev.urlshortener.service;

import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.dto.CreateUrlResponse;
import org.springframework.stereotype.Service;

@Service
public class UrlService {

    public CreateUrlResponse shortenUrl(CreateUrlRequest request) {
        if (!request.url().startsWith("http://") && !request.url().startsWith("https://"))
            throw new IllegalArgumentException();

        return new CreateUrlResponse(request.url());
    }

}
