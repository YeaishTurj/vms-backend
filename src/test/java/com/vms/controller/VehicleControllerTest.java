package com.vms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vms.dto.VehicleRequest;
import com.vms.dto.VehicleResponse;
import com.vms.exception.DuplicateVehicleNumberException;
import com.vms.exception.GlobalExceptionHandler;
import com.vms.exception.ResourceNotFoundException;
import com.vms.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vms.security.CustomUserDetailsService;
import com.vms.security.JwtAccessDeniedHandler;
import com.vms.security.JwtAuthenticationEntryPoint;
import com.vms.security.JwtAuthenticationFilter;
import com.vms.security.JwtTokenProvider;
import com.vms.security.SecurityConfig;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(VehicleController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
@WithMockUser
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private VehicleRequest validRequest;
    private VehicleResponse sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = new VehicleRequest(
                "ABC-1234",
                "Apex Logistics",
                "Semi-Truck",
                "Freightliner",
                "Cascadia",
                "Active"
        );

        sampleResponse = new VehicleResponse(
                1L,
                "ABC-1234",
                "Apex Logistics",
                "Semi-Truck",
                "Freightliner",
                "Cascadia",
                "Active"
        );
    }

    @Nested
    @DisplayName("GET /vehicles (Pagination & Lookup)")
    class GetVehiclesTests {

        @Test
        @DisplayName("GET /vehicles?page=0&size=10 returns paginated list with required Page fields")
        void getVehicles_paginated_shouldReturnPageResponse() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            PageImpl<VehicleResponse> pageResult = new PageImpl<>(List.of(sampleResponse), pageable, 1);
            when(vehicleService.getVehicles(any(Pageable.class))).thenReturn(pageResult);

            mockMvc.perform(get("/vehicles")
                            .param("page", "0")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", notNullValue()))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].vehicleNumber", is("ABC-1234")))
                    .andExpect(jsonPath("$.content[0].company", is("Apex Logistics")))
                    .andExpect(jsonPath("$.number", is(0)))
                    .andExpect(jsonPath("$.size", is(10)))
                    .andExpect(jsonPath("$.totalElements", is(1)))
                    .andExpect(jsonPath("$.totalPages", is(1)))
                    .andExpect(jsonPath("$.last", is(true)));
        }

        @Test
        @DisplayName("GET /api/v1/vehicles also resolves and returns paginated list")
        void getVehicles_versionedRoute_shouldSucceed() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            PageImpl<VehicleResponse> pageResult = new PageImpl<>(List.of(sampleResponse), pageable, 1);
            when(vehicleService.getVehicles(any(Pageable.class))).thenReturn(pageResult);

            mockMvc.perform(get("/api/v1/vehicles")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].vehicleNumber", is("ABC-1234")));
        }

        @Test
        @DisplayName("GET /vehicles with negative page number returns 400 Bad Request")
        void getVehicles_withNegativePage_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/vehicles")
                            .param("page", "-1")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("Page index must not be less than zero")));
        }

        @Test
        @DisplayName("GET /vehicles with invalid zero size returns 400 Bad Request")
        void getVehicles_withZeroSize_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/vehicles")
                            .param("page", "0")
                            .param("size", "0")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("Page size must be at least 1")));
        }

        @Test
        @DisplayName("GET /vehicles with excessive size returns 400 Bad Request")
        void getVehicles_withExcessiveSize_shouldReturnBadRequest() throws Exception {
            mockMvc.perform(get("/vehicles")
                            .param("size", "101")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.message", containsString("Page size must not exceed 100")));
        }

        @Test
        @DisplayName("GET /vehicles/{id} returns single vehicle by ID")
        void getVehicleById_whenExists_shouldReturnVehicle() throws Exception {
            when(vehicleService.getVehicleById(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/vehicles/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.vehicleNumber", is("ABC-1234")))
                    .andExpect(jsonPath("$.company", is("Apex Logistics")))
                    .andExpect(jsonPath("$.type", is("Semi-Truck")))
                    .andExpect(jsonPath("$.brand", is("Freightliner")))
                    .andExpect(jsonPath("$.model", is("Cascadia")))
                    .andExpect(jsonPath("$.status", is("Active")));
        }

        @Test
        @DisplayName("GET /vehicles/{id} returns 404 when vehicle ID does not exist")
        void getVehicleById_whenNotFound_shouldReturn404() throws Exception {
            when(vehicleService.getVehicleById(999L))
                    .thenThrow(new ResourceNotFoundException("Vehicle not found with ID: 999"));

            mockMvc.perform(get("/vehicles/999")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", containsString("Vehicle not found with ID: 999")));
        }
    }

    @Nested
    @DisplayName("POST /vehicles (Creation & Validation)")
    class CreateVehicleTests {

        @Test
        @DisplayName("POST /vehicles with valid request creates vehicle and returns 201 Created")
        void createVehicle_withValidData_shouldReturn201() throws Exception {
            when(vehicleService.createVehicle(any(VehicleRequest.class))).thenReturn(sampleResponse);

            mockMvc.perform(post("/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", containsString("/vehicles/1")))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.vehicleNumber", is("ABC-1234")))
                    .andExpect(jsonPath("$.company", is("Apex Logistics")));
        }

        @Test
        @DisplayName("POST /vehicles with duplicate vehicle number returns 409 Conflict")
        void createVehicle_withDuplicateNumber_shouldReturn409() throws Exception {
            when(vehicleService.createVehicle(any(VehicleRequest.class)))
                    .thenThrow(new DuplicateVehicleNumberException("Vehicle number 'ABC-1234' is already registered."));

            mockMvc.perform(post("/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Duplicate Vehicle Number")))
                    .andExpect(jsonPath("$.message", containsString("already registered")));
        }

        @Test
        @DisplayName("POST /vehicles with missing required fields returns 400 Bad Request")
        void createVehicle_withMissingFields_shouldReturn400() throws Exception {
            VehicleRequest emptyRequest = new VehicleRequest();

            mockMvc.perform(post("/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation Failed")))
                    .andExpect(jsonPath("$.details.vehicleNumber", notNullValue()))
                    .andExpect(jsonPath("$.details.company", notNullValue()))
                    .andExpect(jsonPath("$.details.type", notNullValue()))
                    .andExpect(jsonPath("$.details.brand", notNullValue()))
                    .andExpect(jsonPath("$.details.model", notNullValue()))
                    .andExpect(jsonPath("$.details.status", notNullValue()));
        }
    }

    @Nested
    @DisplayName("PUT /vehicles/{id} (Update & Validation)")
    class UpdateVehicleTests {

        @Test
        @DisplayName("PUT /vehicles/{id} with valid data updates vehicle and returns 200 OK")
        void updateVehicle_withValidData_shouldReturn200() throws Exception {
            VehicleResponse updatedResponse = new VehicleResponse(
                    1L,
                    "ABC-1234",
                    "Apex Global Logistics",
                    "Semi-Truck",
                    "Freightliner",
                    "Cascadia Evolution",
                    "Under Maintenance"
            );
            when(vehicleService.updateVehicle(eq(1L), any(VehicleRequest.class))).thenReturn(updatedResponse);

            mockMvc.perform(put("/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.company", is("Apex Global Logistics")))
                    .andExpect(jsonPath("$.model", is("Cascadia Evolution")))
                    .andExpect(jsonPath("$.status", is("Under Maintenance")));
        }

        @Test
        @DisplayName("PUT /vehicles/{id} with non-existent ID returns 404 Not Found")
        void updateVehicle_whenNotFound_shouldReturn404() throws Exception {
            when(vehicleService.updateVehicle(eq(999L), any(VehicleRequest.class)))
                    .thenThrow(new ResourceNotFoundException("Vehicle not found with ID: 999"));

            mockMvc.perform(put("/vehicles/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }

        @Test
        @DisplayName("PUT /vehicles/{id} with duplicate vehicle number returns 409 Conflict")
        void updateVehicle_withDuplicateNumber_shouldReturn409() throws Exception {
            when(vehicleService.updateVehicle(eq(1L), any(VehicleRequest.class)))
                    .thenThrow(new DuplicateVehicleNumberException("Vehicle number 'TAKEN-777' is already registered to another vehicle."));

            mockMvc.perform(put("/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Duplicate Vehicle Number")))
                    .andExpect(jsonPath("$.message", containsString("already registered to another vehicle")));
        }

        @Test
        @DisplayName("PUT /vehicles/{id} with blank required field returns 400 Bad Request")
        void updateVehicle_withBlankField_shouldReturn400() throws Exception {
            VehicleRequest invalidRequest = new VehicleRequest(
                    "",
                    "Apex Logistics",
                    "Truck",
                    "Volvo",
                    "FH16",
                    "Active"
            );

            mockMvc.perform(put("/vehicles/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.details.vehicleNumber", notNullValue()));
        }
    }

    @Nested
    @DisplayName("DELETE /vehicles/{id}")
    class DeleteVehicleTests {

        @Test
        @DisplayName("DELETE /vehicles/{id} deletes vehicle and returns 204 No Content")
        void deleteVehicle_whenExists_shouldReturn204() throws Exception {
            doNothing().when(vehicleService).deleteVehicle(1L);

            mockMvc.perform(delete("/vehicles/1"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /vehicles/{id} when non-existent returns 404 Not Found")
        void deleteVehicle_whenNotFound_shouldReturn404() throws Exception {
            doThrow(new ResourceNotFoundException("Vehicle not found with ID: 999"))
                    .when(vehicleService).deleteVehicle(999L);

            mockMvc.perform(delete("/vehicles/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")));
        }
    }

    @Nested
    @DisplayName("Endpoint Security Enforcement")
    class EndpointSecurityTests {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /vehicles without authentication returns 401 Unauthorized")
        void getVehicles_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(get("/vehicles"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error", is("Unauthorized")))
                    .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /vehicles without authentication returns 401 Unauthorized")
        void postVehicle_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(post("/vehicles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error", is("Unauthorized")));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("DELETE /vehicles/{id} without authentication returns 401 Unauthorized")
        void deleteVehicle_unauthenticated_shouldReturn401() throws Exception {
            mockMvc.perform(delete("/vehicles/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.error", is("Unauthorized")));
        }
    }
}
