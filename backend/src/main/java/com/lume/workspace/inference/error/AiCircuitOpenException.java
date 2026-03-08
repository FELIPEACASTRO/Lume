package com.lume.workspace.inference.error;

public class AiCircuitOpenException extends AiProviderException {

    public AiCircuitOpenException(String message) {
        super(message, true);
    }
}
