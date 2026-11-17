package com.vms.service;

import com.vms.dto.VehicleRequest;
import com.vms.dto.VehicleResponse;
import com.vms.entity.Vehicle;
import com.vms.exception.DuplicateVehicleNumberException;
import com.vms.exception.ResourceNotFoundException;
import com.vms.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing vehicle lifecycle, business rule validations, and data persistence.
 * Enforces business logic such as duplicate vehicle number prevention and safe DTO transformation.
 */
@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    /**
     * Registers a new vehicle in the system.
     * Rejects creation if a vehicle with the same vehicle number already exists.
     *
     * @param request the vehicle registration request
     * @return the created VehicleResponse DTO
     * @throws DuplicateVehicleNumberException if the vehicle number is already registered
     */
    public VehicleResponse createVehicle(VehicleRequest request) {
        String vehicleNumber = sanitize(request.getVehicleNumber());

        if (vehicleRepository.existsByVehicleNumberIgnoreCase(vehicleNumber)) {
            throw new DuplicateVehicleNumberException(
                    "Vehicle number '" + vehicleNumber + "' is already registered."
            );
        }

        Vehicle vehicle = new Vehicle(
                vehicleNumber,
                sanitize(request.getCompany()),
                sanitize(request.getType()),
                sanitize(request.getBrand()),
                sanitize(request.getModel()),
                sanitize(request.getStatus())
        );

        Vehicle saved = vehicleRepository.save(vehicle);
        return VehicleResponse.fromEntity(saved);
    }

    /**
     * Updates an existing vehicle's information.
     * Rejects the update if the vehicle number is in use by another vehicle.
     * Allowing an update if the vehicle number matches the target vehicle's own current number.
     *
     * @param id the ID of the vehicle to update
     * @param request the updated vehicle details
     * @return the updated VehicleResponse DTO
     * @throws ResourceNotFoundException if no vehicle exists with the given ID
     * @throws DuplicateVehicleNumberException if the vehicle number belongs to another vehicle
     */
    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        Vehicle existingVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + id));

        String newVehicleNumber = sanitize(request.getVehicleNumber());

        Optional<Vehicle> duplicateCandidate = vehicleRepository.findByVehicleNumberIgnoreCase(newVehicleNumber);
        if (duplicateCandidate.isPresent() && !duplicateCandidate.get().getId().equals(id)) {
            throw new DuplicateVehicleNumberException(
                    "Vehicle number '" + newVehicleNumber + "' is already registered to another vehicle."
            );
        }

        existingVehicle.setVehicleNumber(newVehicleNumber);
        existingVehicle.setCompany(sanitize(request.getCompany()));
        existingVehicle.setType(sanitize(request.getType()));
        existingVehicle.setBrand(sanitize(request.getBrand()));
        existingVehicle.setModel(sanitize(request.getModel()));
        existingVehicle.setStatus(sanitize(request.getStatus()));

        Vehicle updated = vehicleRepository.save(existingVehicle);
        return VehicleResponse.fromEntity(updated);
    }

    /**
     * Retrieves a vehicle by its unique ID.
     *
     * @param id the ID of the vehicle
     * @return the VehicleResponse DTO
     * @throws ResourceNotFoundException if no vehicle exists with the given ID
     */
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + id));
        return VehicleResponse.fromEntity(vehicle);
    }

    /**
     * Retrieves all registered vehicles as DTOs.
     *
     * @return list of VehicleResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll().stream()
                .map(VehicleResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a vehicle by its ID.
     *
     * @param id the ID of the vehicle to delete
     * @throws ResourceNotFoundException if no vehicle exists with the given ID
     */
    public void deleteVehicle(Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Vehicle not found with ID: " + id);
        }
        vehicleRepository.deleteById(id);
    }

    private String sanitize(String value) {
        return value != null ? value.trim() : null;
    }
}
