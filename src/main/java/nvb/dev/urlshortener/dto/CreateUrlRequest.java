package nvb.dev.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUrlRequest(
        @NotBlank(message = "url cannot be blank.")
        String url) {
}
