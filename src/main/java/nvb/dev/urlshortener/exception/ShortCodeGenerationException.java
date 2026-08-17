package nvb.dev.urlshortener.exception;

public class ShortCodeGenerationException extends RuntimeException {

    public ShortCodeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShortCodeGenerationException(String message) {
        super(message);
    }
}
