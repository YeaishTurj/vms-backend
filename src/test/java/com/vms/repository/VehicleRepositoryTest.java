package com.vms.repository;

import com.vms.entity.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Vehicle persistedVehicle;

    @BeforeEach
    void setUp() {
        Vehicle vehicle = new Vehicle(
                "ABC-1001",
                "Metro Express",
                "Delivery Van",
                "Ford",
                "Transit",
                "Active"
        );
        persistedVehicle = entityManager.persistAndFlush(vehicle);
    }

    @Test
    @DisplayName("findByVehicleNumberIgnoreCase finds matching vehicle regardless of casing")
    void findByVehicleNumberIgnoreCase_shouldFindVehicle() {
        Optional<Vehicle> foundUpper = vehicleRepository.findByVehicleNumberIgnoreCase("ABC-1001");
        Optional<Vehicle> foundLower = vehicleRepository.findByVehicleNumberIgnoreCase("abc-1001");

        assertThat(foundUpper).isPresent();
        assertThat(foundUpper.get().getId()).isEqualTo(persistedVehicle.getId());
        assertThat(foundLower).isPresent();
        assertThat(foundLower.get().getId()).isEqualTo(persistedVehicle.getId());
    }

    @Test
    @DisplayName("findByVehicleNumberIgnoreCase returns empty when vehicle number does not exist")
    void findByVehicleNumberIgnoreCase_whenNotFound_shouldReturnEmpty() {
        Optional<Vehicle> found = vehicleRepository.findByVehicleNumberIgnoreCase("NON-EXISTENT");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByVehicleNumberIgnoreCase returns true when vehicle exists, false otherwise")
    void existsByVehicleNumberIgnoreCase_shouldCheckExistence() {
        boolean exists = vehicleRepository.existsByVehicleNumberIgnoreCase("abc-1001");
        boolean notExists = vehicleRepository.existsByVehicleNumberIgnoreCase("UNKNOWN");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Database unique constraint rejects persisting duplicate vehicle numbers")
    void persist_duplicateVehicleNumber_shouldThrowDataIntegrityViolationException() {
        Vehicle duplicate = new Vehicle(
                "ABC-1001", // Duplicate vehicle number
                "Another Logistics",
                "Truck",
                "Volvo",
                "FH16",
                "Active"
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            vehicleRepository.saveAndFlush(duplicate);
        });
    }
}
