package com.usc.lugarlangfinal.models;

import com.google.firebase.database.PropertyName;

public class Trip {

    // 1. Move the Annotations to the FIELDS.
    // 2. This tells Firebase EXACTLY which key to look for.
    @PropertyName("tripId")
    private String tripId;

    @PropertyName("routeCode")
    private String routeCode;

    @PropertyName("vehicleCode")
    private String vehicleCode;

    @PropertyName("PlateNumber")
    private String plateNumber;

    @PropertyName("terminal1")
    private String terminal1;

    @PropertyName("terminal2")
    private String terminal2;

    @PropertyName("driverName")
    private String driverName;

    @PropertyName("conductorName")
    private String conductorName;

    @PropertyName("departureTime")
    private String departureTime;

    @PropertyName("status")
    private String status;

    @PropertyName("franchise")
    private String franchise;

    @PropertyName("t1_Coords")
    private String t1_Coords;

    @PropertyName("t2_Coords")
    private String t2_Coords;

    @PropertyName("Stops")
    private String stops;

    @PropertyName("Stop_Coords")
    private String stops_Coords;

    @PropertyName("AssignedTransport")
    private String assignedTransport;

    public Trip() {}

    // 3. Remove all @PropertyName from these Getters and Setters.
    // They will now just act as normal Java methods to access the annotated fields.

    public String getTripId() { return tripId; }
    public void setTripId(String tripId) { this.tripId = tripId; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getRouteCode() { return routeCode; }
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    public String getVehicleCode() { return vehicleCode; }
    public void setVehicleCode(String vehicleCode) { this.vehicleCode = vehicleCode; }

    public String getTerminal1() { return terminal1; }
    public void setTerminal1(String terminal1) { this.terminal1 = terminal1; }

    public String getTerminal2() { return terminal2; }
    public void setTerminal2(String terminal2) { this.terminal2 = terminal2; }

    public String getT1_Coords() { return t1_Coords; }
    public void setT1_Coords(String t1_Coords) { this.t1_Coords = t1_Coords; }

    public String getT2_Coords() { return t2_Coords; }
    public void setT2_Coords(String t2_Coords) { this.t2_Coords = t2_Coords; }

    public String getStops() { return stops; }
    public void setStops(String stops) { this.stops = stops; }

    public String getStops_Coords() { return stops_Coords; }
    public void setStops_Coords(String stops_Coords) { this.stops_Coords = stops_Coords; }

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

    public String getAssignedTransport() { return assignedTransport; }
    public void setAssignedTransport(String assignedTransport) { this.assignedTransport = assignedTransport; }
}