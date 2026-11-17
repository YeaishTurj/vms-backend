package com.vms.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller verifying backend availability and SQL database connectivity.
 * Exposes both '/api/v1/health' and root '/health' for container/cloud platform probes (e.g. Render).
 */
@RestController
@RequestMapping({"/api/v1/health", "/health"})
public class DatabaseHealthController {

    private final DataSource dataSource;

    public DatabaseHealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "vms-backend");
        response.put("timestamp", Instant.now().toString());

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            Map<String, Object> dbInfo = new HashMap<>();
            dbInfo.put("status", "CONNECTED");
            dbInfo.put("databaseProduct", metaData.getDatabaseProductName());
            dbInfo.put("databaseVersion", metaData.getDatabaseProductVersion());
            dbInfo.put("driverName", metaData.getDriverName());
            dbInfo.put("driverVersion", metaData.getDriverVersion());
            response.put("database", dbInfo);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> dbInfo = new HashMap<>();
            dbInfo.put("status", "DISCONNECTED");
            dbInfo.put("error", e.getMessage());
            response.put("database", dbInfo);
            response.put("status", "DOWN");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
