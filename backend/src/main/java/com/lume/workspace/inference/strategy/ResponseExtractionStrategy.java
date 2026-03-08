package com.lume.workspace.inference.strategy;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface ResponseExtractionStrategy {

    String extractContent(JsonNode response);
}
