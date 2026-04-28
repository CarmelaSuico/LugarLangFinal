package com.usc.lugarlangfinal.models;

import com.google.firebase.database.PropertyName;

public class Trip {
    public String tripId, routeCode, vehicleCode, terminal1, terminal2, driverName, conductorName, departureTime, status, franchise;

    // Use PropertyName to match the Capital 'P' in Firebase
    @PropertyName("PlateNumber")
    public String plateNumber;

    public Trip() {} // Required for Firebase

    // Add these specifically for PlateNumber to ensure Firebase reads/writes correctly
    @PropertyName("PlateNumber")
    public String getPlateNumber() { return plateNumber; }

    @PropertyName("PlateNumber")
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }
}