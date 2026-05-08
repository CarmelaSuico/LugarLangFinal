package com.usc.lugarlangfinal.models;

import com.google.firebase.database.PropertyName;
import java.io.Serializable;

public class Trip implements Serializable {

    private String tripId = "";
    private String routeCode = "";
    private String vehicleCode = "";
    private String plateNumber = "";
    private String terminal1 = "";
    private String terminal2 = "";
    private String driverName = "N/A";
    private String conductorName = "N/A";
    private String departureTime = "--:--";
    private String status = "Unknown";
    private String franchise = "";
    private String t1_Coords = "0,0";
    private String t2_Coords = "0,0";
    private String stops = "";
    private String stops_Coords = "";
    private String assignedTransport = "";

    public Trip() {} // Required for Firebase

    @PropertyName("tripId")
    public String getTripId() { return tripId; }
    @PropertyName("tripId")
    public void setTripId(String v) { this.tripId = v != null ? v : ""; }

    @PropertyName("RouteCode")
    public String getRouteCode() { return routeCode; }
    @PropertyName("RouteCode")
    public void setRouteCode(String v) { this.routeCode = v != null ? v : ""; }

    @PropertyName("VehicleCode")
    public String getVehicleCode() { return vehicleCode; }
    @PropertyName("VehicleCode")
    public void setVehicleCode(String v) { this.vehicleCode = v != null ? v : ""; }

    @PropertyName("PlateNumber")
    public String getPlateNumber() { return plateNumber; }
    @PropertyName("PlateNumber")
    public void setPlateNumber(String v) { this.plateNumber = v != null ? v : ""; }

    @PropertyName("Terminal1")
    public String getTerminal1() { return terminal1; }
    @PropertyName("Terminal1")
    public void setTerminal1(String v) { this.terminal1 = v != null ? v : ""; }

    @PropertyName("Terminal2")
    public String getTerminal2() { return terminal2; }
    @PropertyName("Terminal2")
    public void setTerminal2(String v) { this.terminal2 = v != null ? v : ""; }

    @PropertyName("driverName")
    public String getDriverName() { return driverName; }
    @PropertyName("driverName")
    public void setDriverName(String v) { this.driverName = v != null ? v : "N/A"; }

    @PropertyName("conductorName")
    public String getConductorName() { return conductorName; }
    @PropertyName("conductorName")
    public void setConductorName(String v) { this.conductorName = v != null ? v : "N/A"; }

    @PropertyName("departureTime")
    public String getDepartureTime() { return departureTime; }
    @PropertyName("departureTime")
    public void setDepartureTime(String v) { this.departureTime = v != null ? v : "--:--"; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String v) { this.status = v != null ? v : "Unknown"; }

    @PropertyName("franchise")
    public String getFranchise() { return franchise; }
    @PropertyName("franchise")
    public void setFranchise(String v) { this.franchise = v != null ? v : ""; }

    @PropertyName("T1_Coords")
    public String getT1Coords() { return t1_Coords; }
    @PropertyName("T1_Coords")
    public void setT1Coords(String v) { this.t1_Coords = v != null ? v : "0,0"; }

    @PropertyName("T2_Coords")
    public String getT2Coords() { return t2_Coords; }
    @PropertyName("T2_Coords")
    public void setT2Coords(String v) { this.t2_Coords = v != null ? v : "0,0"; }

    @PropertyName("Stops")
    public String getStops() { return stops; }
    @PropertyName("Stops")
    public void setStops(String v) { this.stops = v != null ? v : ""; }

    @PropertyName("Stop_Coords")
    public String getStopCoords() { return stops_Coords; }
    @PropertyName("Stop_Coords")
    public void setStopCoords(String v) { this.stops_Coords = v != null ? v : ""; }

    @PropertyName("AssignedTransport")
    public String getAssignedTransport() { return assignedTransport; }
    @PropertyName("AssignedTransport")
    public void setAssignedTransport(String v) { this.assignedTransport = v != null ? v : ""; }
}