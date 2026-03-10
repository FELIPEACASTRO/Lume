package com.lume.workspace.inference.error;

public class AiUnsupportedOperationException extends AiProviderException {

    public AiUnsupportedOperationException(String message) {
        super(message, false);
    }
}
