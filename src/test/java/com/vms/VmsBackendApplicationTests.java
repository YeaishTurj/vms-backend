package com.vms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VmsBackendApplicationTests {

    @Test
    @DisplayName("Spring Boot application context loads successfully")
    void contextLoads() {
        // Confirms that Spring Boot context, JPA repositories, and configuration load without errors
    }
}
