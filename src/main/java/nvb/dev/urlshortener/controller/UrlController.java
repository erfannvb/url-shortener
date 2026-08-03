package nvb.dev.urlshortener.controller;

import nvb.dev.urlshortener.dto.CreateUrlRequest;
import nvb.dev.urlshortener.dto.CreateUrlResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/urls")
public class UrlController {

    @PostMapping
    public ResponseEntity<CreateUrlResponse> shortenUrl(@RequestBody CreateUrlRequest createUrlRequest) {
        return new ResponseEntity<>(toResponse(createUrlRequest), HttpStatus.CREATED);
    }

    private CreateUrlResponse toResponse(CreateUrlRequest request) {
        String url = request.url();
        return new CreateUrlResponse(url);
    }

}
