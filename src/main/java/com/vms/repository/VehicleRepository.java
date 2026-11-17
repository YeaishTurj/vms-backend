package com.vms.repository;

import com.vms.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Vehicle} entity.
 * Provides data access operations and query methods for vehicle lookup and validation.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    /**
     * Finds a vehicle by its vehicle number, ignoring case.
     *
     * @param vehicleNumber the vehicle number to search for
     * @return an Optional containing the matching Vehicle, or empty if not found
     */
    Optional<Vehicle> findByVehicleNumberIgnoreCase(String vehicleNumber);

    /**
     * Checks if a vehicle already exists with the given vehicle number, ignoring case.
     *
     * @param vehicleNumber the vehicle number to check
     * @return true if a vehicle with the number exists, false otherwise
     */
    boolean existsByVehicleNumberIgnoreCase(String vehicleNumber);
}
