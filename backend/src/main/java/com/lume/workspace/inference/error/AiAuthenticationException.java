package com.lume.workspace.inference.error;

public class AiAuthenticationException extends AiProviderException {

    public AiAuthenticationException(String message) {
        super(message, false);
    }
}
