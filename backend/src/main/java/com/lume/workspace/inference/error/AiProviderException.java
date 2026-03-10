package com.lume.workspace.inference.error;

public class AiProviderException extends RuntimeException {

    private final boolean retryable;

    public AiProviderException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public AiProviderException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
