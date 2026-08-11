package nvb.dev.urlshortener.controller;

import lombok.RequiredArgsConstructor;
import nvb.dev.urlshortener.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;

    @GetMapping(path = "/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable(name = "shortCode") String shortCode) {
        String originalUrl = urlService.resolveShortCode(shortCode);
        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

}
