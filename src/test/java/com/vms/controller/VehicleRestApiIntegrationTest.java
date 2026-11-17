package com.vms.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vms.dto.LoginRequest;
import com.vms.dto.VehicleRequest;
import com.vms.entity.Vehicle;
import com.vms.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Complete integration test suite for the VMS Vehicle REST APIs.
 * Integrates the HTTP/Controller layer, Security filters, VehicleService,
 * VehicleRepository, and the configured SQL database end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VehicleRestApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VehicleRepository vehicleRepository;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure a clean database state for each test to guarantee repeatability and independence
        vehicleRepository.deleteAll();

        // Obtain real JWT bearer token via the authentication endpoint
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        authToken = "Bearer " + jsonNode.get("token").asText();
    }

    // =========================================================================
    // 1. CREATE: POST /vehicles
    // =========================================================================

    @Nested
    @DisplayName("1. CREATE - POST /vehicles")
    class CreateVehicleTests {

        @Test
        @DisplayName("POST /vehicles with valid information creates vehicle, returns 201 Created and persists in SQL database")
        void createVehicle_validData_returns201AndPersists() throws Exception {
            VehicleRequest request = new VehicleRequest(
                    "TRK-1001",
                    "Pacific Freight Inc",
                    "Semi-Truck",
                    "Freightliner",
                    "Cascadia",
                    "Active"
            );

            MvcResult result = mockMvc.perform(post("/vehicles")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/vehicles/")))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.vehicleNumber", is("TRK-1001")))
                    .andExpect(jsonPath("$.company", is("Pacific Freight Inc")))
                    .andExpect(jsonPath("$.type", is("Semi-Truck")))
                    .andExpect(jsonPath("$.brand", is("Freightliner")))
                    .andExpect(jsonPath("$.model", is("Cascadia")))
                    .andExpect(jsonPath("$.status", is("Active")))
                    .andReturn();

            Long createdId = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asLong();

            // Verify persistence directly in the SQL database
            Optional<Vehicle> savedInDb = vehicleRepository.findById(createdId);
            assertThat(savedInDb).isPresent();
            assertThat(savedInDb.get().getVehicleNumber()).isEqualTo("TRK-1001");
            assertThat(savedInDb.get().getCompany()).isEqualTo("Pacific Freight Inc");
            assertThat(savedInDb.get().getType()).isEqualTo("Semi-Truck");
            assertThat(savedInDb.get().getBrand()).isEqualTo("Freightliner");
            assertThat(savedInDb.get().getModel()).isEqualTo("Cascadia");
            assertThat(savedInDb.get().getStatus()).isEqualTo("Active");
        }

        @Test
        @DisplayName("POST /api/v1/vehicles versioned alias creates vehicle successfully")
        void createVehicle_viaVersionedEndpoint_returns201() throws Exception {
            VehicleRequest request = new VehicleRequest(
                    "VAN-2002",
                    "Metro Express Delivery",
                    "Cargo Van",
                    "Mercedes-Benz",
                    "Sprinter",
                    "Active"
            );

            mockMvc.perform(post("/api/v1/vehicles")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.vehicleNumber", is("VAN-2002")))
                    .andExpect(jsonPath("$.company", is("Metro Express Delivery")));

            assertThat(vehicleRepository.existsByVehicleNumberIgnoreCase("VAN-2002")).isTrue();
        }
    }

    // =========================================================================
    // 2. READ: GET /vehicles & GET /vehicles/{id}
    // =========================================================================

    @Nested
    @DisplayName("2. READ - GET /vehicles & GET /vehicles/{id}")
    class ReadVehicleTests {

        @Test
        @DisplayName("GET /vehicles?page=0&size=10 returns paginated list with required pagination metadata")
        void getVehicles_paginated_returnsPaginationFieldsAndContent() throws Exception {
            // Seed 3 vehicles in database
            vehicleRepository.save(new Vehicle("FLT-01", "Fleet Co", "Van", "Ford", "Transit", "Active"));
            vehicleRepository.save(new Vehicle("FLT-02", "Fleet Co", "Truck", "Volvo", "VNL", "Active"));
            vehicleRepository.save(new Vehicle("FLT-03", "Fleet Co", "Sedan", "Toyota", "Camry", "Inactive"));

            mockMvc.perform(get("/vehicles?page=0&size=10")
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    // Verify required Spring Page JSON response fields
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.number", is(0)))
                    .andExpect(jsonPath("$.size", is(10)))
                    .andExpect(jsonPath("$.totalElements", is(3)))
                    .andExpect(jsonPath("$.totalPages", is(1)))
                    .andExpect(jsonPath("$.last", is(true)))
                    .andExpect(jsonPath("$.content[0].vehicleNumber", is("FLT-01")))
                    .andExpect(jsonPath("$.content[1].vehicleNumber", is("FLT-02")))
                    .andExpect(jsonPath("$.content[2].vehicleNumber", is("FLT-03")));
        }

        @Test
        @DisplayName("GET /vehicles/{id} returns the specific vehicle record by ID")
        void getVehicleById_existingId_returnsVehicle() throws Exception {
            Vehicle vehicle = vehicleRepository.save(new Vehicle(
                    "EXP-3003",
                    "Global Express",
                    "Flatbed Truck",
                    "Kenworth",
                    "T680",
                    "Active"
            ));

            mockMvc.perform(get("/vehicles/" + vehicle.getId())
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(vehicle.getId().intValue())))
                    .andExpect(jsonPath("$.vehicleNumber", is("EXP-3003")))
                    .andExpect(jsonPath("$.company", is("Global Express")))
                    .andExpect(jsonPath("$.type", is("Flatbed Truck")))
                    .andExpect(jsonPath("$.brand", is("Kenworth")))
                    .andExpect(jsonPath("$.model", is("T680")))
                    .andExpect(jsonPath("$.status", is("Active")));
        }
    }

    // =========================================================================
    // 3. UPDATE: PUT /vehicles/{id}
    // =========================================================================

    @Nested
    @DisplayName("3. UPDATE - PUT /vehicles/{id}")
    class UpdateVehicleTests {

        @Test
        @DisplayName("PUT /vehicles/{id} updates vehicle information, returns 200, and verifies persistence on subsequent GET and in SQL DB")
        void updateVehicle_validChanges_updatesAndPersists() throws Exception {
            // Seed initial vehicle
            Vehicle initial = vehicleRepository.save(new Vehicle(
                    "MOD-4001",
                    "Initial Logistics",
                    "Van",
                    "Ford",
                    "Transit",
                    "Active"
            ));
            Long id = initial.getId();

            // Prepare update request
            VehicleRequest updateRequest = new VehicleRequest(
                    "MOD-4001", // retaining its own vehicle number
                    "Updated Worldwide Logistics",
                    "Electric Van",
                    "Mercedes-Benz",
                    "eSprinter",
                    "Maintenance"
            );

            // Execute PUT
            mockMvc.perform(put("/vehicles/" + id)
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(id.intValue())))
                    .andExpect(jsonPath("$.vehicleNumber", is("MOD-4001")))
                    .andExpect(jsonPath("$.company", is("Updated Worldwide Logistics")))
                    .andExpect(jsonPath("$.type", is("Electric Van")))
                    .andExpect(jsonPath("$.brand", is("Mercedes-Benz")))
                    .andExpect(jsonPath("$.model", is("eSprinter")))
                    .andExpect(jsonPath("$.status", is("Maintenance")));

            // Fetch it again via GET /vehicles/{id} and confirm updated information persists
            mockMvc.perform(get("/vehicles/" + id)
                            .header("Authorization", authToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.company", is("Updated Worldwide Logistics")))
                    .andExpect(jsonPath("$.type", is("Electric Van")))
                    .andExpect(jsonPath("$.brand", is("Mercedes-Benz")))
                    .andExpect(jsonPath("$.model", is("eSprinter")))
                    .andExpect(jsonPath("$.status", is("Maintenance")));

            // Confirm updated information in SQL database
            Vehicle fromDb = vehicleRepository.findById(id).orElseThrow();
            assertThat(fromDb.getCompany()).isEqualTo("Updated Worldwide Logistics");
            assertThat(fromDb.getType()).isEqualTo("Electric Van");
            assertThat(fromDb.getBrand()).isEqualTo("Mercedes-Benz");
            assertThat(fromDb.getModel()).isEqualTo("eSprinter");
            assertThat(fromDb.getStatus()).isEqualTo("Maintenance");
        }
    }

    // =========================================================================
    // 4. DELETE: DELETE /vehicles/{id}
    // =========================================================================

    @Nested
    @DisplayName("4. DELETE - DELETE /vehicles/{id}")
    class DeleteVehicleTests {

        @Test
        @DisplayName("DELETE /vehicles/{id} returns 204 No Content, subsequent GET returns 404, and record is deleted from SQL DB")
        void deleteVehicle_existingId_deletesSuccessfully() throws Exception {
            Vehicle vehicle = vehicleRepository.save(new Vehicle(
                    "DEL-5001",
                    "Disposal Fleet",
                    "Trailer",
                    "Utility",
                    "4000D-X",
                    "Decommissioned"
            ));
            Long id = vehicle.getId();

            // Execute DELETE /vehicles/{id}
            mockMvc.perform(delete("/vehicles/" + id)
                            .header("Authorization", authToken))
                    .andExpect(status().isNoContent());

            // Fetch the record again via GET /vehicles/{id} and confirm it no longer exists (404)
            mockMvc.perform(get("/vehicles/" + id)
                            .header("Authorization", authToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", containsString("Vehicle not found with ID: " + id)));

            // Confirm directly in SQL database that record no longer exists
            assertThat(vehicleRepository.findById(id)).isEmpty();
        }
    }

    // =========================================================================
    // 5. DUPLICATE VALIDATION: HTTP 409 Conflict
    // =========================================================================

    @Nested
    @DisplayName("5. DUPLICATE VALIDATION - HTTP 409 Conflict")
    class DuplicateValidationTests {

        @Test
        @DisplayName("POST /vehicles with duplicate Vehicle Number returns HTTP 409 Conflict with informative error message")
        void createDuplicateVehicle_returns409Conflict() throws Exception {
            // Seed initial vehicle
            vehicleRepository.save(new Vehicle(
                    "DUP-6001",
                    "First Transport",
                    "Box Truck",
                    "Isuzu",
                    "NPR",
                    "Active"
            ));

            // Attempt to create another vehicle with the exact same vehicle number
            VehicleRequest duplicateRequest = new VehicleRequest(
                    "DUP-6001",
                    "Second Transport",
                    "Cargo Van",
                    "Ford",
                    "Transit",
                    "Active"
            );

            mockMvc.perform(post("/vehicles")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Duplicate Vehicle Number")))
                    .andExpect(jsonPath("$.message", containsString("Vehicle number 'DUP-6001' is already registered")));

            // Also test case-insensitive duplicate: "dup-6001"
            VehicleRequest caseInsensitiveDuplicate = new VehicleRequest(
                    "dup-6001",
                    "Third Transport",
                    "Cargo Van",
                    "Ford",
                    "Transit",
                    "Active"
            );

            mockMvc.perform(post("/vehicles")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(caseInsensitiveDuplicate)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Duplicate Vehicle Number")));

            // Ensure only one record exists in database
            assertThat(vehicleRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("PUT /vehicles/{id} with another vehicle's Vehicle Number returns HTTP 409 Conflict")
        void updateVehicle_withExistingVehicleNumber_returns409Conflict() throws Exception {
            Vehicle vehicle1 = vehicleRepository.save(new Vehicle(
                    "V1-NUMBER",
                    "First Logistics",
                    "Van",
                    "Ford",
                    "Transit",
                    "Active"
            ));

            Vehicle vehicle2 = vehicleRepository.save(new Vehicle(
                    "V2-NUMBER",
                    "Second Logistics",
                    "Truck",
                    "Volvo",
                    "VNL",
                    "Active"
            ));

            // Attempt to update vehicle2 to have vehicle1's number
            VehicleRequest duplicateUpdateRequest = new VehicleRequest(
                    "V1-NUMBER",
                    "Second Logistics",
                    "Truck",
                    "Volvo",
                    "VNL",
                    "Active"
            );

            mockMvc.perform(put("/vehicles/" + vehicle2.getId())
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateUpdateRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Duplicate Vehicle Number")))
                    .andExpect(jsonPath("$.message", containsString("is already registered to another vehicle")));
        }
    }

    // =========================================================================
    // 6. FIELD VALIDATION: HTTP 400 Bad Request
    // =========================================================================

    @Nested
    @DisplayName("6. FIELD VALIDATION - HTTP 400 Bad Request")
    class FieldValidationTests {

        @Test
        @DisplayName("POST /vehicles with missing required fields returns HTTP 400 Bad Request with field-level error messages")
        void createVehicle_missingRequiredFields_returns400WithDetails() throws Exception {
            VehicleRequest invalidRequest = new VehicleRequest(
                    "", // blank vehicleNumber
                    "", // blank company
                    "", // blank type
                    "", // blank brand
                    "", // blank model
                    ""  // blank status
            );

            mockMvc.perform(post("/vehicles")
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.message", is("Validation failed for one or more fields")))
                    .andExpect(jsonPath("$.details.vehicleNumber", is("Vehicle number is required")))
                    .andExpect(jsonPath("$.details.company", is("Company is required")))
                    .andExpect(jsonPath("$.details.type", is("Vehicle type is required")))
                    .andExpect(jsonPath("$.details.brand", is("Brand is required")))
                    .andExpect(jsonPath("$.details.model", is("Model is required")))
                    .andExpect(jsonPath("$.details.status", is("Status is required")));

            // Ensure no invalid records were created in the database
            assertThat(vehicleRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("PUT /vehicles/{id} with missing required fields returns HTTP 400 Bad Request")
        void updateVehicle_missingRequiredFields_returns400() throws Exception {
            Vehicle vehicle = vehicleRepository.save(new Vehicle(
                    "VAL-7001",
                    "Alpha Logistics",
                    "Van",
                    "Ford",
                    "Transit",
                    "Active"
            ));

            VehicleRequest invalidRequest = new VehicleRequest(
                    null, // null vehicleNumber
                    "Alpha Logistics",
                    "Van",
                    "Ford",
                    "Transit",
                    "Active"
            );

            mockMvc.perform(put("/vehicles/" + vehicle.getId())
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.details.vehicleNumber", is("Vehicle number is required")));
        }

        @Test
        @DisplayName("GET /vehicles with invalid page or size parameters returns HTTP 400 Bad Request")
        void getVehicles_invalidPaginationParams_returns400() throws Exception {
            mockMvc.perform(get("/vehicles?page=-1&size=10")
                            .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("Page index must not be less than zero")));

            mockMvc.perform(get("/vehicles?page=0&size=0")
                            .header("Authorization", authToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("Page size must be at least 1")));
        }
    }

    // =========================================================================
    // 7. NOT FOUND: HTTP 404
    // =========================================================================

    @Nested
    @DisplayName("7. NOT FOUND - HTTP 404")
    class NotFoundTests {

        @Test
        @DisplayName("GET /vehicles/{id} for non-existent vehicle returns HTTP 404 Not Found")
        void getVehicleById_nonExistentId_returns404() throws Exception {
            Long nonExistentId = 999999L;

            mockMvc.perform(get("/vehicles/" + nonExistentId)
                            .header("Authorization", authToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", containsString("Vehicle not found with ID: " + nonExistentId)));
        }

        @Test
        @DisplayName("PUT /vehicles/{id} for non-existent vehicle returns HTTP 404 Not Found")
        void updateVehicle_nonExistentId_returns404() throws Exception {
            Long nonExistentId = 999999L;
            VehicleRequest request = new VehicleRequest(
                    "NON-EXIST",
                    "Ghost Fleet",
                    "Van",
                    "Ford",
                    "Transit",
                    "Active"
            );

            mockMvc.perform(put("/vehicles/" + nonExistentId)
                            .header("Authorization", authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", containsString("Vehicle not found with ID: " + nonExistentId)));
        }

        @Test
        @DisplayName("DELETE /vehicles/{id} for non-existent vehicle returns HTTP 404 Not Found")
        void deleteVehicle_nonExistentId_returns404() throws Exception {
            Long nonExistentId = 999999L;

            mockMvc.perform(delete("/vehicles/" + nonExistentId)
                            .header("Authorization", authToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", containsString("Vehicle not found with ID: " + nonExistentId)));
        }
    }
}
