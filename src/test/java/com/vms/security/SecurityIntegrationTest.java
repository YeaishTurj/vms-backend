package com.vms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vms.dto.LoginRequest;
import com.vms.dto.VehicleRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Health endpoint is public and requires no authentication")
    void healthEndpoint_isPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Unauthenticated GET /vehicles is rejected with 401 Unauthorized")
    void getVehicles_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/vehicles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Unauthenticated POST /vehicles is rejected with 401 Unauthorized")
    void postVehicle_withoutAuth_returns401() throws Exception {
        VehicleRequest vehicle = new VehicleRequest("SEC-001", "Corp", "Van", "Ford", "Transit", "Active");

        mockMvc.perform(post("/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicle)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Unauthenticated PUT /vehicles/{id} is rejected with 401 Unauthorized")
    void putVehicle_withoutAuth_returns401() throws Exception {
        VehicleRequest vehicle = new VehicleRequest("SEC-001", "Corp", "Van", "Ford", "Transit", "Active");

        mockMvc.perform(put("/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(vehicle)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Unauthenticated DELETE /vehicles/{id} is rejected with 401 Unauthorized")
    void deleteVehicle_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/vehicles/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Request with invalid/tampered JWT token is rejected with 401 Unauthorized")
    void requestWithInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/vehicles")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.invalid.signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("Full authentication lifecycle: login, obtain token, access protected vehicle endpoints successfully")
    void fullAuthLifecycle_allowsProtectedAccess() throws Exception {
        // Step 1: Login to acquire JWT token
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andReturn();

        String token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();

        // Step 2: Use Bearer token to create a vehicle
        VehicleRequest newVehicle = new VehicleRequest(
                "AUTH-7777",
                "Apex Security",
                "SUV",
                "Chevrolet",
                "Tahoe",
                "Active"
        );

        MvcResult createResult = mockMvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newVehicle)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vehicleNumber", is("AUTH-7777")))
                .andReturn();

        Long createdId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Step 3: Fetch vehicle by ID with Bearer token
        mockMvc.perform(get("/vehicles/" + createdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleNumber", is("AUTH-7777")));

        // Step 4: Fetch paginated vehicles with Bearer token
        mockMvc.perform(get("/vehicles?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(emptyOrNullString())));

        // Step 5: Delete vehicle with Bearer token
        mockMvc.perform(delete("/vehicles/" + createdId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
