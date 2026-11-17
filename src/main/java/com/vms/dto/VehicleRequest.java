package com.vms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Data Transfer Object for creating or updating a Vehicle.
 * Encapsulates client input with declarative validation constraints.
 */
public class VehicleRequest {

    @NotBlank(message = "Vehicle number is required")
    @Size(max = 64, message = "Vehicle number must not exceed 64 characters")
    private String vehicleNumber;

    @NotBlank(message = "Company is required")
    @Size(max = 128, message = "Company name must not exceed 128 characters")
    private String company;

    @NotBlank(message = "Vehicle type is required")
    @Size(max = 64, message = "Vehicle type must not exceed 64 characters")
    private String type;

    @NotBlank(message = "Brand is required")
    @Size(max = 64, message = "Brand must not exceed 64 characters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 64, message = "Model must not exceed 64 characters")
    private String model;

    @NotBlank(message = "Status is required")
    @Size(max = 32, message = "Status must not exceed 32 characters")
    private String status;

    public VehicleRequest() {
    }

    public VehicleRequest(String vehicleNumber, String company, String type, String brand, String model, String status) {
        this.vehicleNumber = vehicleNumber;
        this.company = company;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.status = status;
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
        VehicleRequest that = (VehicleRequest) o;
        return Objects.equals(vehicleNumber, that.vehicleNumber) &&
                Objects.equals(company, that.company) &&
                Objects.equals(type, that.type) &&
                Objects.equals(brand, that.brand) &&
                Objects.equals(model, that.model) &&
                Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vehicleNumber, company, type, brand, model, status);
    }

    @Override
    public String toString() {
        return "VehicleRequest{" +
                "vehicleNumber='" + vehicleNumber + '\'' +
                ", company='" + company + '\'' +
                ", type='" + type + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
