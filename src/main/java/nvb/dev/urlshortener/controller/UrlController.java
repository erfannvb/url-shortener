package nvb.dev.urlshortener.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.dto.CreateUrlResponse;
import nvb.dev.urlshortener.service.UrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping(path = "/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping
    public ResponseEntity<CreateUrlResponse> shortenUrl(@Valid @RequestBody CreateUrlRequest createUrlRequest) {
        CreateUrlResponse response = urlService.shortenUrl(createUrlRequest);

        URI uri = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/{shortCode}")
                .buildAndExpand(response.shortenedUrl())
                .toUri();

        CreateUrlResponse finalResponse = new CreateUrlResponse(uri.toString());

        return ResponseEntity.created(uri)
                .body(finalResponse);
    }

}
