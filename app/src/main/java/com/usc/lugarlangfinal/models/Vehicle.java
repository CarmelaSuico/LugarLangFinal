package com.usc.lugarlangfinal.models;
import com.google.firebase.database.PropertyName;

public class Vehicle {
    private String vehicleCode, plateNumber, vehicleType, franchise, status;

    public Vehicle() {}

    @PropertyName("VehicleCode")
    public String getVehicleCode() { return vehicleCode; }
    @PropertyName("VehicleCode")
    public void setVehicleCode(String vehicleCode) { this.vehicleCode = vehicleCode; }

    @PropertyName("PlateNumber")
    public String getPlateNumber() { return plateNumber; }
    @PropertyName("PlateNumber")
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    @PropertyName("VehicleType")
    public String getVehicleType() { return vehicleType; }
    @PropertyName("VehicleType")
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    @PropertyName("Franchise")
    public String getFranchise() { return franchise; }
    @PropertyName("Franchise")
    public void setFranchise(String franchise) { this.franchise = franchise; }

    @PropertyName("Status")
    public String getStatus() { return status; }
    @PropertyName("Status")
    public void setStatus(String status) { this.status = status; }
}