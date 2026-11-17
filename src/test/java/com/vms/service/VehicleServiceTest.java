package com.vms.service;

import com.vms.dto.VehicleRequest;
import com.vms.dto.VehicleResponse;
import com.vms.entity.Vehicle;
import com.vms.exception.DuplicateVehicleNumberException;
import com.vms.exception.ResourceNotFoundException;
import com.vms.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle sampleVehicle;
    private VehicleRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleVehicle = new Vehicle(
                1L,
                "XYZ-1234",
                "Apex Logistics",
                "Semi-Truck",
                "Freightliner",
                "Cascadia",
                "Active"
        );

        sampleRequest = new VehicleRequest(
                "XYZ-1234",
                "Apex Logistics",
                "Semi-Truck",
                "Freightliner",
                "Cascadia",
                "Active"
        );
    }

    @Nested
    @DisplayName("Create Vehicle Tests")
    class CreateVehicleTests {

        @Test
        @DisplayName("New Vehicle Number accepted: successfully saves vehicle and returns DTO")
        void createVehicle_withNewVehicleNumber_shouldSaveAndReturnResponse() {
            when(vehicleRepository.existsByVehicleNumberIgnoreCase("XYZ-1234")).thenReturn(false);
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
                Vehicle v = invocation.getArgument(0);
                v.setId(10L);
                return v;
            });

            VehicleResponse response = vehicleService.createVehicle(sampleRequest);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getVehicleNumber()).isEqualTo("XYZ-1234");
            assertThat(response.getCompany()).isEqualTo("Apex Logistics");
            assertThat(response.getType()).isEqualTo("Semi-Truck");
            assertThat(response.getBrand()).isEqualTo("Freightliner");
            assertThat(response.getModel()).isEqualTo("Cascadia");
            assertThat(response.getStatus()).isEqualTo("Active");

            verify(vehicleRepository).existsByVehicleNumberIgnoreCase("XYZ-1234");
            verify(vehicleRepository).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("Existing Vehicle Number rejected: throws DuplicateVehicleNumberException without saving")
        void createVehicle_withExistingVehicleNumber_shouldThrowDuplicateException() {
            when(vehicleRepository.existsByVehicleNumberIgnoreCase("XYZ-1234")).thenReturn(true);

            DuplicateVehicleNumberException ex = assertThrows(
                    DuplicateVehicleNumberException.class,
                    () -> vehicleService.createVehicle(sampleRequest)
            );

            assertThat(ex.getMessage()).contains("XYZ-1234");
            assertThat(ex.getMessage()).contains("already registered");

            verify(vehicleRepository).existsByVehicleNumberIgnoreCase("XYZ-1234");
            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("Whitespace in Vehicle Number is trimmed during creation")
        void createVehicle_withUntrimmedNumber_shouldTrimAndCheck() {
            VehicleRequest untrimmedRequest = new VehicleRequest(
                    "  XYZ-1234  ",
                    "Apex Logistics",
                    "Truck",
                    "Volvo",
                    "FH16",
                    "Active"
            );

            when(vehicleRepository.existsByVehicleNumberIgnoreCase("XYZ-1234")).thenReturn(false);
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

            VehicleResponse response = vehicleService.createVehicle(untrimmedRequest);

            assertThat(response.getVehicleNumber()).isEqualTo("XYZ-1234");
            verify(vehicleRepository).existsByVehicleNumberIgnoreCase("XYZ-1234");
        }
    }

    @Nested
    @DisplayName("Update Vehicle Tests")
    class UpdateVehicleTests {

        @Test
        @DisplayName("Updating a vehicle with its own existing Vehicle Number is allowed")
        void updateVehicle_withOwnVehicleNumber_shouldUpdateSuccessfully() {
            Long vehicleId = 1L;
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(sampleVehicle));
            when(vehicleRepository.findByVehicleNumberIgnoreCase("XYZ-1234")).thenReturn(Optional.of(sampleVehicle));
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

            VehicleRequest updateRequest = new VehicleRequest(
                    "XYZ-1234",
                    "Apex Global Logistics",
                    "Semi-Truck",
                    "Freightliner",
                    "Cascadia Evolution",
                    "In Maintenance"
            );

            VehicleResponse response = vehicleService.updateVehicle(vehicleId, updateRequest);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(vehicleId);
            assertThat(response.getVehicleNumber()).isEqualTo("XYZ-1234");
            assertThat(response.getCompany()).isEqualTo("Apex Global Logistics");
            assertThat(response.getModel()).isEqualTo("Cascadia Evolution");
            assertThat(response.getStatus()).isEqualTo("In Maintenance");

            verify(vehicleRepository).save(sampleVehicle);
        }

        @Test
        @DisplayName("Updating a vehicle with a completely new unassigned Vehicle Number is allowed")
        void updateVehicle_withNewAvailableNumber_shouldUpdateSuccessfully() {
            Long vehicleId = 1L;
            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(sampleVehicle));
            when(vehicleRepository.findByVehicleNumberIgnoreCase("NEW-9999")).thenReturn(Optional.empty());
            when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

            VehicleRequest updateRequest = new VehicleRequest(
                    "NEW-9999",
                    "Apex Logistics",
                    "Semi-Truck",
                    "Freightliner",
                    "Cascadia",
                    "Active"
            );

            VehicleResponse response = vehicleService.updateVehicle(vehicleId, updateRequest);

            assertThat(response).isNotNull();
            assertThat(response.getVehicleNumber()).isEqualTo("NEW-9999");
            verify(vehicleRepository).save(sampleVehicle);
        }

        @Test
        @DisplayName("Updating to another vehicle's Vehicle Number is rejected with DuplicateVehicleNumberException")
        void updateVehicle_withAnotherVehiclesNumber_shouldThrowDuplicateException() {
            Long vehicleId = 1L;
            Vehicle anotherVehicle = new Vehicle(
                    2L,
                    "TAKEN-777",
                    "Other Corp",
                    "Van",
                    "Ford",
                    "Transit",
                    "Active"
            );

            when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(sampleVehicle));
            when(vehicleRepository.findByVehicleNumberIgnoreCase("TAKEN-777")).thenReturn(Optional.of(anotherVehicle));

            VehicleRequest updateRequest = new VehicleRequest(
                    "TAKEN-777",
                    "Apex Logistics",
                    "Semi-Truck",
                    "Freightliner",
                    "Cascadia",
                    "Active"
            );

            DuplicateVehicleNumberException ex = assertThrows(
                    DuplicateVehicleNumberException.class,
                    () -> vehicleService.updateVehicle(vehicleId, updateRequest)
            );

            assertThat(ex.getMessage()).contains("TAKEN-777");
            assertThat(ex.getMessage()).contains("already registered to another vehicle");

            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }

        @Test
        @DisplayName("Updating non-existent vehicle throws ResourceNotFoundException")
        void updateVehicle_whenVehicleDoesNotExist_shouldThrowNotFoundException() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> vehicleService.updateVehicle(999L, sampleRequest)
            );

            verify(vehicleRepository, never()).save(any(Vehicle.class));
        }
    }

    @Nested
    @DisplayName("Query & Delete Tests")
    class QueryAndDeleteTests {

        @Test
        @DisplayName("getVehicleById returns matching DTO when vehicle exists")
        void getVehicleById_whenExists_shouldReturnResponse() {
            when(vehicleRepository.findById(1L)).thenReturn(Optional.of(sampleVehicle));

            VehicleResponse response = vehicleService.getVehicleById(1L);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getVehicleNumber()).isEqualTo("XYZ-1234");
        }

        @Test
        @DisplayName("getVehicleById throws ResourceNotFoundException when vehicle not found")
        void getVehicleById_whenNotFound_shouldThrowNotFoundException() {
            when(vehicleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> vehicleService.getVehicleById(999L)
            );
        }

        @Test
        @DisplayName("getAllVehicles returns list of all mapped VehicleResponse DTOs")
        void getAllVehicles_shouldReturnListOfResponses() {
            Vehicle vehicle2 = new Vehicle(
                    2L,
                    "DEF-5678",
                    "Metro Delivery",
                    "Van",
                    "Mercedes",
                    "Sprinter",
                    "Active"
            );

            when(vehicleRepository.findAll()).thenReturn(List.of(sampleVehicle, vehicle2));

            List<VehicleResponse> allVehicles = vehicleService.getAllVehicles();

            assertThat(allVehicles).hasSize(2);
            assertThat(allVehicles.get(0).getVehicleNumber()).isEqualTo("XYZ-1234");
            assertThat(allVehicles.get(1).getVehicleNumber()).isEqualTo("DEF-5678");
        }

        @Test
        @DisplayName("deleteVehicle deletes successfully when vehicle exists")
        void deleteVehicle_whenExists_shouldDelete() {
            when(vehicleRepository.existsById(1L)).thenReturn(true);

            vehicleService.deleteVehicle(1L);

            verify(vehicleRepository).deleteById(1L);
        }

        @Test
        @DisplayName("deleteVehicle throws ResourceNotFoundException when vehicle does not exist")
        void deleteVehicle_whenNotFound_shouldThrowNotFoundException() {
            when(vehicleRepository.existsById(999L)).thenReturn(false);

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> vehicleService.deleteVehicle(999L)
            );

            verify(vehicleRepository, never()).deleteById(any());
        }
    }
}
