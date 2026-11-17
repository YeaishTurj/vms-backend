package com.vms.dto;

import com.vms.entity.Vehicle;

import java.util.Objects;

/**
 * Data Transfer Object returned to API clients representing vehicle data.
 * Prevents direct exposure of JPA internal state and entity bindings.
 */
public class VehicleResponse {

    private Long id;
    private String vehicleNumber;
    private String company;
    private String type;
    private String brand;
    private String model;
    private String status;

    public VehicleResponse() {
    }

    public VehicleResponse(Long id, String vehicleNumber, String company, String type, String brand, String model, String status) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.company = company;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.status = status;
    }

    /**
     * Converts a {@link Vehicle} entity into a {@link VehicleResponse} DTO.
     *
     * @param vehicle the vehicle entity
     * @return the mapped VehicleResponse DTO, or null if entity is null
     */
    public static VehicleResponse fromEntity(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getVehicleNumber(),
                vehicle.getCompany(),
                vehicle.getType(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getStatus()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleResponse that = (VehicleResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(vehicleNumber, that.vehicleNumber) &&
                Objects.equals(company, that.company) &&
                Objects.equals(type, that.type) &&
                Objects.equals(brand, that.brand) &&
                Objects.equals(model, that.model) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vehicleNumber, company, type, brand, model, status);
    }

    @Override
    public String toString() {
        return "VehicleResponse{" +
                "id=" + id +
                ", vehicleNumber='" + vehicleNumber + '\'' +
                ", company='" + company + '\'' +
                ", type='" + type + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
