package net.deltaclient;

public class CompletionException extends RuntimeException {
    public CompletionException() {
        super();
    }

    public CompletionException(String message) {
        super(message);
    }

    public CompletionException(Throwable cause) {
        super(cause);
    }

    public CompletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
