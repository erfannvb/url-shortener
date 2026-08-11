package nvb.dev.urlshortener.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorApiResponse {
    private String message;
    private int statusCode;
    private LocalDateTime timestamp;
}
