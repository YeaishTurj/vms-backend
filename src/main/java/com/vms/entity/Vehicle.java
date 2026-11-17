package com.vms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Objects;

/**
 * Vehicle entity representing a managed vehicle in the VMS system.
 * Persisted in the 'vehicles' table with a database-enforced unique constraint
 * on the vehicle_number column.
 */
@Entity
@Table(
        name = "vehicles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vehicles_vehicle_number", columnNames = {"vehicle_number"})
        }
)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_number", nullable = false, unique = true, length = 64)
    private String vehicleNumber;

    @Column(name = "company", nullable = false, length = 128)
    private String company;

    @Column(name = "type", nullable = false, length = 64)
    private String type;

    @Column(name = "brand", nullable = false, length = 64)
    private String brand;

    @Column(name = "model", nullable = false, length = 64)
    private String model;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    public Vehicle() {
    }

    public Vehicle(String vehicleNumber, String company, String type, String brand, String model, String status) {
        this.vehicleNumber = vehicleNumber;
        this.company = company;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.status = status;
    }

    public Vehicle(Long id, String vehicleNumber, String company, String type, String brand, String model, String status) {
        this.id = id;
        this.vehicleNumber = vehicleNumber;
        this.company = company;
        this.type = type;
        this.brand = brand;
        this.model = model;
        this.status = status;
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
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(id, vehicle.id) ||
                (vehicleNumber != null && vehicleNumber.equalsIgnoreCase(vehicle.vehicleNumber));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vehicleNumber != null ? vehicleNumber.toUpperCase() : null);
    }

    @Override
    public String toString() {
        return "Vehicle{" +
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
