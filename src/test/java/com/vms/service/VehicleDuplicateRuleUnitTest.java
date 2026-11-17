package com.vms.service;

import com.vms.dto.VehicleRequest;
import com.vms.dto.VehicleResponse;
import com.vms.entity.Vehicle;
import com.vms.exception.DuplicateVehicleNumberException;
import com.vms.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit test suite for the Vehicle Number uniqueness and duplicate prevention business rules.
 * <p>
 * Technical specifications:
 * - Pure unit tests powered by JUnit 5 and Mockito.
 * - Zero dependency on live SQL databases or external network services.
 * - Verifies business contract behaviors, exception types, and error messages.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Vehicle Number Uniqueness & Duplicate Rule Unit Tests")
class VehicleDuplicateRuleUnitTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    private Vehicle existingVehicle;

    @BeforeEach
    void setUp() {
        existingVehicle = new Vehicle(
                1L,
                "DM-GA-11-2041",
                "Apex Logistics",
                "Sedan",
                "Toyota",
                "Camry",
                "Active"
        );
    }

    /**
     * Test 1:
     * When a new vehicle is submitted with a Vehicle Number that already exists,
     * the service must reject it with the expected duplicate exception.
     */
    @Test
    @DisplayName("Test 1: When a new vehicle has an existing Vehicle Number, it must be rejected with DuplicateVehicleNumberException")
    void test1_createVehicle_whenVehicleNumberAlreadyExists_shouldRejectWithDuplicateException() {
        // Arrange
        String duplicateNumber = "DM-GA-11-2041";
        VehicleRequest request = new VehicleRequest(
                duplicateNumber,
                "Metro Express",
                "Van",
                "Ford",
                "Transit",
                "Active"
        );

        when(vehicleRepository.existsByVehicleNumberIgnoreCase(duplicateNumber)).thenReturn(true);

        // Act & Assert
        DuplicateVehicleNumberException exception = assertThrows(
                DuplicateVehicleNumberException.class,
                () -> vehicleService.createVehicle(request),
                "Expected createVehicle to throw DuplicateVehicleNumberException when vehicle number already exists"
        );

        // Verify business contract and error message
        assertThat(exception.getMessage())
                .as("Exception message must reference the offending duplicate vehicle number")
                .contains(duplicateNumber)
                .contains("already registered");

        // Verify that the repository save operation was never invoked
        verify(vehicleRepository).existsByVehicleNumberIgnoreCase(duplicateNumber);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    /**
     * Test 2:
     * When a new vehicle has a unique Vehicle Number, the service must accept it.
     */
    @Test
    @DisplayName("Test 2: When a new vehicle has a unique Vehicle Number, the service must accept it and persist the record")
    void test2_createVehicle_whenVehicleNumberIsUnique_shouldAcceptVehicle() {
        // Arrange
        String uniqueNumber = "UNIQUE-9999";
        VehicleRequest request = new VehicleRequest(
                uniqueNumber,
                "Apex Logistics",
                "SUV",
                "Toyota",
                "RAV4",
                "Active"
        );

        when(vehicleRepository.existsByVehicleNumberIgnoreCase(uniqueNumber)).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle toSave = invocation.getArgument(0);
            toSave.setId(100L);
            return toSave;
        });

        // Act
        VehicleResponse response = vehicleService.createVehicle(request);

        // Assert
        assertThat(response).as("Created vehicle response should not be null").isNotNull();
        assertThat(response.getId()).as("Generated primary key should be populated").isEqualTo(100L);
        assertThat(response.getVehicleNumber()).isEqualTo(uniqueNumber);
        assertThat(response.getCompany()).isEqualTo("Apex Logistics");
        assertThat(response.getType()).isEqualTo("SUV");
        assertThat(response.getBrand()).isEqualTo("Toyota");
        assertThat(response.getModel()).isEqualTo("RAV4");
        assertThat(response.getStatus()).isEqualTo("Active");

        // Verify repository interactions
        verify(vehicleRepository).existsByVehicleNumberIgnoreCase(uniqueNumber);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    /**
     * Test 3:
     * When updating a vehicle, its existing Vehicle Number must be allowed.
     */
    @Test
    @DisplayName("Test 3: When updating a vehicle, retaining its own existing Vehicle Number must be allowed")
    void test3_updateVehicle_whenRetainingExistingVehicleNumber_shouldAllowUpdate() {
        // Arrange
        Long targetId = 1L;
        String sameNumber = "DM-GA-11-2041";
        VehicleRequest updateRequest = new VehicleRequest(
                sameNumber,
                "Apex Logistics International",
                "Sedan",
                "Toyota",
                "Camry Hybrid 2026",
                "Under Maintenance"
        );

        when(vehicleRepository.findById(targetId)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.findByVehicleNumberIgnoreCase(sameNumber)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VehicleResponse response = vehicleService.updateVehicle(targetId, updateRequest);

        // Assert
        assertThat(response).as("Updated vehicle response should not be null").isNotNull();
        assertThat(response.getId()).isEqualTo(targetId);
        assertThat(response.getVehicleNumber()).isEqualTo(sameNumber);
        assertThat(response.getCompany()).isEqualTo("Apex Logistics International");
        assertThat(response.getModel()).isEqualTo("Camry Hybrid 2026");
        assertThat(response.getStatus()).isEqualTo("Under Maintenance");

        // Verify save was invoked on the modified entity
        verify(vehicleRepository).findById(targetId);
        verify(vehicleRepository).findByVehicleNumberIgnoreCase(sameNumber);
        verify(vehicleRepository).save(existingVehicle);
    }

    /**
     * Test 4:
     * When updating a vehicle with another vehicle's Vehicle Number, the update must be rejected.
     */
    @Test
    @DisplayName("Test 4: When updating a vehicle with another vehicle's Vehicle Number, the update must be rejected")
    void test4_updateVehicle_whenUsingAnotherVehiclesNumber_shouldRejectWithDuplicateException() {
        // Arrange
        Long targetId = 1L;
        String takenNumber = "TAKEN-7777";
        Vehicle anotherVehicle = new Vehicle(
                2L, // Different vehicle ID!
                takenNumber,
                "Prime Fleet",
                "Truck",
                "Volvo",
                "VNL",
                "Active"
        );

        VehicleRequest updateRequest = new VehicleRequest(
                takenNumber,
                "Apex Logistics",
                "Sedan",
                "Toyota",
                "Camry",
                "Active"
        );

        when(vehicleRepository.findById(targetId)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.findByVehicleNumberIgnoreCase(takenNumber)).thenReturn(Optional.of(anotherVehicle));

        // Act & Assert
        DuplicateVehicleNumberException exception = assertThrows(
                DuplicateVehicleNumberException.class,
                () -> vehicleService.updateVehicle(targetId, updateRequest),
                "Expected updateVehicle to throw DuplicateVehicleNumberException when vehicle number belongs to another ID"
        );

        // Verify business contract and error message
        assertThat(exception.getMessage())
                .as("Exception message must identify the conflict with another vehicle")
                .contains(takenNumber)
                .contains("already registered to another vehicle");

        // Verify repository save was never called
        verify(vehicleRepository).findById(targetId);
        verify(vehicleRepository).findByVehicleNumberIgnoreCase(takenNumber);
        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    /**
     * Supporting Test: Case-insensitivity verification.
     * Rejects creation when matching existing number in a different case (e.g. lowercase vs uppercase).
     */
    @Test
    @DisplayName("Supporting Test: Duplicate rejection enforces case-insensitivity on creation")
    void createVehicle_withCaseInsensitiveDuplicateNumber_shouldReject() {
        // Arrange
        String lowercaseDuplicate = "dm-ga-11-2041";
        VehicleRequest request = new VehicleRequest(
                lowercaseDuplicate,
                "Metro Express",
                "Van",
                "Ford",
                "Transit",
                "Active"
        );

        when(vehicleRepository.existsByVehicleNumberIgnoreCase(lowercaseDuplicate)).thenReturn(true);

        // Act & Assert
        DuplicateVehicleNumberException exception = assertThrows(
                DuplicateVehicleNumberException.class,
                () -> vehicleService.createVehicle(request)
        );

        assertThat(exception.getMessage()).contains(lowercaseDuplicate);
        verify(vehicleRepository, never()).save(any());
    }

    /**
     * Supporting Test: Updating vehicle with a completely new, unassigned vehicle number succeeds.
     */
    @Test
    @DisplayName("Supporting Test: Updating vehicle with a completely new unassigned number is accepted")
    void updateVehicle_withCompletelyNewUnassignedNumber_shouldUpdateSuccessfully() {
        // Arrange
        Long targetId = 1L;
        String brandNewNumber = "BRAND-NEW-100";
        VehicleRequest updateRequest = new VehicleRequest(
                brandNewNumber,
                "Apex Logistics",
                "Sedan",
                "Toyota",
                "Camry",
                "Active"
        );

        when(vehicleRepository.findById(targetId)).thenReturn(Optional.of(existingVehicle));
        when(vehicleRepository.findByVehicleNumberIgnoreCase(brandNewNumber)).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VehicleResponse response = vehicleService.updateVehicle(targetId, updateRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getVehicleNumber()).isEqualTo(brandNewNumber);
        verify(vehicleRepository).save(existingVehicle);
    }
}
