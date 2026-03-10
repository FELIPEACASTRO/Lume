package com.lume.workspace.inference.error;

public class AiRateLimitException extends AiProviderException {

    public AiRateLimitException(String message) {
        super(message, true);
    }
}
