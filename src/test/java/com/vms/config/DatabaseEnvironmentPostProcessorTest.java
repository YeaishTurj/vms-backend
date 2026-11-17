package com.vms.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseEnvironmentPostProcessorTest {

    private final DatabaseEnvironmentPostProcessor processor = new DatabaseEnvironmentPostProcessor();
    private final SpringApplication application = new SpringApplication();

    @Test
    @DisplayName("Converts Render postgres:// URI into standard jdbc:postgresql:// with credentials")
    void convertsRenderPostgresUriWithCredentials() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testEnv", Map.of(
                "DATABASE_URL", "postgres://vms_user:mySecretPassword123@dpg-abc123456-a:5432/vms_db"
        )));

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-abc123456-a:5432/vms_db");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("vms_user");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("mySecretPassword123");
    }

    @Test
    @DisplayName("Defaults port to 5432 if port is omitted in Render URL")
    void defaultsPortTo5432IfOmitted() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testEnv", Map.of(
                "INTERNAL_DATABASE_URL", "postgres://render_user:pass456@dpg-host-a/prod_vms"
        )));

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-host-a:5432/prod_vms");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("render_user");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("pass456");
    }

    @Test
    @DisplayName("Leaves standard JDBC URLs untouched")
    void leavesStandardJdbcUrlUntouched() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testEnv", Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/vms_db"
        )));

        processor.postProcessEnvironment(environment, application);

        assertThat(environment.getProperty("spring.datasource.url")).isNull(); // No override added
    }
}
