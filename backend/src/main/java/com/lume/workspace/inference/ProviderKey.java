package com.lume.workspace.inference;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum ProviderKey {
    OPENAI("openai"),
    GEMINI("google-gemini", "gemini"),
    DEEPSEEK("deepseek"),
    ANTHROPIC("anthropic", "claude"),
    XAI("xai", "grok"),
    PERPLEXITY("perplexity"),
    GROQ("groq"),
    OPENROUTER("openrouter"),
    COHERE("cohere"),
    TOGETHER("together"),
    FIREWORKS("fireworks"),
    DEEPINFRA("deepinfra"),
    MISTRAL("mistral");

    private final String code;
    private final Set<String> aliases;

    ProviderKey(String code, String... aliases) {
        this.code = code;
        this.aliases = Set.copyOf(Arrays.asList(aliases));
    }

    public String code() {
        return code;
    }

    public boolean matches(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return code.equals(normalized) || aliases.contains(normalized);
    }

    public static Optional<ProviderKey> from(String value) {
        return Arrays.stream(values())
                .filter(key -> key.matches(value))
                .findFirst();
    }
}
