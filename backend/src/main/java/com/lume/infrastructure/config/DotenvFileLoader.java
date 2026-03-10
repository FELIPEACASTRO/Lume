package com.lume.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DotenvFileLoader {

    public static final String PROPERTY_SOURCE_NAME = "lumeDotenv";

    private static final Logger log = LoggerFactory.getLogger(DotenvFileLoader.class);
    private static final List<Path> CANDIDATES = List.of(
            Paths.get(".env"),
            Paths.get("..", ".env"),
            Paths.get(System.getProperty("user.dir", "."), ".env"),
            Paths.get(System.getProperty("user.dir", "."), "..", ".env")
    );

    private DotenvFileLoader() {
    }

    public static Map<String, Object> load() {
        for (Path candidate : CANDIDATES) {
            Map<String, Object> properties = load(candidate);
            if (!properties.isEmpty()) {
                return properties;
            }
        }
        return Map.of();
    }

    public static void register(ConfigurableEnvironment environment) {
        Map<String, Object> values = load();
        if (values.isEmpty()) {
            log.debug("Nenhum arquivo .env local encontrado para bootstrap.");
            return;
        }

        MutablePropertySources propertySources = environment.getPropertySources();
        if (propertySources.contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, values);
        if (propertySources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            propertySources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
        } else {
            propertySources.addLast(propertySource);
        }

        log.info("Bootstrap local de .env carregado com {} variaveis, sem sobrescrever o ambiente do processo.", values.size());
    }

    private static Map<String, Object> load(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return Map.of();
        }

        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                parseLine(rawLine).ifPresent(entry -> values.putIfAbsent(entry.getKey(), entry.getValue()));
            }
        } catch (IOException exception) {
            log.warn("Nao foi possivel ler o arquivo .env local em {}: {}", path.toAbsolutePath(), exception.getMessage());
            return Map.of();
        }

        if (!values.isEmpty()) {
            log.info("Arquivo .env local detectado em {}.", path.toAbsolutePath());
        }
        return values;
    }

    private static java.util.Optional<Map.Entry<String, String>> parseLine(String rawLine) {
        if (!StringUtils.hasText(rawLine)) {
            return java.util.Optional.empty();
        }

        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return java.util.Optional.empty();
        }

        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }

        int separatorIndex = line.indexOf('=');
        if (separatorIndex <= 0) {
            return java.util.Optional.empty();
        }

        String key = line.substring(0, separatorIndex).trim();
        String value = line.substring(separatorIndex + 1).trim();

        if (key.isEmpty()) {
            return java.util.Optional.empty();
        }

        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        return java.util.Optional.of(Map.entry(key, value));
    }
}
