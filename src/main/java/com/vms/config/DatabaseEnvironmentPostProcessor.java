package com.vms.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Automatically detects and converts cloud PostgreSQL URLs (e.g. Render / Heroku 'postgres://' or 'postgresql://')
 * into the standard 'jdbc:postgresql://' format expected by Spring Boot, and extracts credentials if not set.
 */
public class DatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String[] candidateKeys = {"DATABASE_URL", "INTERNAL_DATABASE_URL", "DB_URL", "SPRING_DATASOURCE_URL"};
        String rawUrl = null;

        for (String key : candidateKeys) {
            String val = environment.getProperty(key);
            if (val != null && !val.trim().isEmpty()) {
                rawUrl = val.trim();
                break;
            }
        }

        if (rawUrl == null) {
            return;
        }

        // Check if rawUrl is in URI format (e.g. postgres://user:password@host/db or postgresql://...) without jdbc:
        if ((rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) && !rawUrl.startsWith("jdbc:")) {
            try {
                // If scheme is 'postgres', replace with 'postgresql' for java.net.URI parsing
                String parseable = rawUrl.startsWith("postgres://")
                        ? "postgresql://" + rawUrl.substring("postgres://".length())
                        : rawUrl;

                URI uri = new URI(parseable);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath() != null ? uri.getPath() : "/vms_db";

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path;
                Map<String, Object> override = new HashMap<>();
                override.put("spring.datasource.url", jdbcUrl);

                if (uri.getUserInfo() != null && uri.getUserInfo().contains(":")) {
                    String[] creds = uri.getUserInfo().split(":", 2);
                    String currentUsername = environment.getProperty("spring.datasource.username");
                    if (currentUsername == null || currentUsername.trim().isEmpty() || currentUsername.equals("postgres")) {
                        override.put("spring.datasource.username", creds[0]);
                    }
                    String currentPassword = environment.getProperty("spring.datasource.password");
                    if (currentPassword == null || currentPassword.trim().isEmpty()) {
                        override.put("spring.datasource.password", creds[1]);
                    }
                }

                environment.getPropertySources().addFirst(new MapPropertySource("renderPostgreSqlUrlConverter", override));
            } catch (Exception ignored) {
                // If URI parsing fails, prepend jdbc:
                Map<String, Object> fallback = new HashMap<>();
                fallback.put("spring.datasource.url", "jdbc:" + rawUrl);
                environment.getPropertySources().addFirst(new MapPropertySource("renderPostgreSqlUrlFallback", fallback));
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
