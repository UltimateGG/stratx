package stratx.exceptions;

public class InvalidCandlestickException extends RuntimeException {
    public InvalidCandlestickException(String message) {
        super(message);
    }

    public InvalidCandlestickException(String message, Throwable cause) {
        super(message, cause);
    }
}
