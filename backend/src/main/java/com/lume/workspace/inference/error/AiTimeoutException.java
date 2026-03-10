package com.lume.workspace.inference.error;

public class AiTimeoutException extends AiProviderException {

    public AiTimeoutException(String message, Throwable cause) {
        super(message, cause, true);
    }

    public AiTimeoutException(String message) {
        super(message, true);
    }
}
