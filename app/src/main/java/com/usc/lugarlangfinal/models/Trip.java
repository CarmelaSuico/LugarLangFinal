package com.usc.lugarlangfinal.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

public class Trip {

    // 1. Change these to private or use @Exclude to stop the conflict
    @Exclude
    public String tripId, routeCode, vehicleCode, plateNumber, terminal1, terminal2;
    @Exclude
    public String driverName, conductorName, departureTime, status, franchise;
    @Exclude
    public String t1_Coords, t2_Coords, stops, stops_Coords;

    public Trip() {} // Required for Firebase

    // 2. Keep your annotated methods; Firebase will use these exclusively
    @PropertyName("PlateNumber")
    public String getPlateNumber() { return plateNumber; }

    @PropertyName("PlateNumber")
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    // 3. For the rest of the fields, Firebase defaults to camelCase if no annotation is present.
    // If your database uses PascalCase for ALL fields, you must add @PropertyName to each getter/setter.

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    public String getVehicleCode() { return vehicleCode; }
    public void setVehicleCode(String vehicleCode) { this.vehicleCode = vehicleCode; }

    public String getTerminal1() { return terminal1; }
    public void setTerminal1(String terminal1) { this.terminal1 = terminal1; }

    public String getTerminal2() { return terminal2; }
    public void setTerminal2(String terminal2) { this.terminal2 = terminal2; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getConductorName() { return conductorName; }
    public void setConductorName(String conductorName) { this.conductorName = conductorName; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFranchise() { return franchise; }
    public void setFranchise(String franchise) { this.franchise = franchise; }

    public String getT1_Coords() { return t1_Coords; }
    public void setT1_Coords(String t1_Coords) { this.t1_Coords = t1_Coords; }

    public String getT2_Coords() { return t2_Coords; }
    public void setT2_Coords(String t2_Coords) { this.t2_Coords = t2_Coords; }

    public String getStops() { return stops; }
    public void setStops(String stops) { this.stops = stops; }

    public String getStops_Coords() { return stops_Coords; }
    public void setStops_Coords(String stops_Coords) { this.stops_Coords = stops_Coords; }
}