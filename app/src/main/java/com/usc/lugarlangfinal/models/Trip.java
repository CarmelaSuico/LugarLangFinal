package com.usc.lugarlangfinal.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

public class Trip {

    // @Exclude on the fields so Firebase only uses the annotated getters/setters
    @Exclude public String tripId, routeCode, vehicleCode, plateNumber, terminal1, terminal2;
    @Exclude public String driverName, conductorName, departureTime, status, franchise;
    @Exclude public String t1_Coords, t2_Coords, stops, stops_Coords;

    public Trip() {}

    // ── Identifiers ──────────────────────────────────────────────────────────

    @PropertyName("tripId")
    public String getTripId() { return tripId; }
    @PropertyName("tripId")
    public void setTripId(String tripId) { this.tripId = tripId; }

    @PropertyName("PlateNumber")
    public String getPlateNumber() { return plateNumber; }
    @PropertyName("PlateNumber")
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    // ── Route / Vehicle ───────────────────────────────────────────────────────

    @PropertyName("routeCode")
    public String getRouteCode() { return routeCode; }
    @PropertyName("routeCode")
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    @PropertyName("vehicleCode")
    public String getVehicleCode() { return vehicleCode; }
    @PropertyName("vehicleCode")
    public void setVehicleCode(String vehicleCode) { this.vehicleCode = vehicleCode; }

    // ── Terminals ─────────────────────────────────────────────────────────────

    @PropertyName("terminal1")
    public String getTerminal1() { return terminal1; }
    @PropertyName("terminal1")
    public void setTerminal1(String terminal1) { this.terminal1 = terminal1; }

    @PropertyName("terminal2")
    public String getTerminal2() { return terminal2; }
    @PropertyName("terminal2")
    public void setTerminal2(String terminal2) { this.terminal2 = terminal2; }

    // ── Coordinates & Stops ───────────────────────────────────────────────────
    // FIX: Use lowercase "t1_Coords" / "t2_Coords" consistently everywhere.
    // This matches what StartEndTrip.java now reads from Firebase.

    @PropertyName("t1_Coords")
    public String getT1_Coords() { return t1_Coords; }
    @PropertyName("t1_Coords")
    public void setT1_Coords(String t1_Coords) { this.t1_Coords = t1_Coords; }

    @PropertyName("t2_Coords")
    public String getT2_Coords() { return t2_Coords; }
    @PropertyName("t2_Coords")
    public void setT2_Coords(String t2_Coords) { this.t2_Coords = t2_Coords; }

    @PropertyName("Stops")
    public String getStops() { return stops; }
    @PropertyName("Stops")
    public void setStops(String stops) { this.stops = stops; }

    @PropertyName("Stop_Coords")
    public String getStops_Coords() { return stops_Coords; }
    @PropertyName("Stop_Coords")
    public void setStops_Coords(String stops_Coords) { this.stops_Coords = stops_Coords; }

    // ── Crew & Trip Info ──────────────────────────────────────────────────────

    @PropertyName("driverName")
    public String getDriverName() { return driverName; }
    @PropertyName("driverName")
    public void setDriverName(String driverName) { this.driverName = driverName; }

    @PropertyName("conductorName")
    public String getConductorName() { return conductorName; }
    @PropertyName("conductorName")
    public void setConductorName(String conductorName) { this.conductorName = conductorName; }

    @PropertyName("departureTime")
    public String getDepartureTime() { return departureTime; }
    @PropertyName("departureTime")
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("franchise")
    public String getFranchise() { return franchise; }
    @PropertyName("franchise")
    public void setFranchise(String franchise) { this.franchise = franchise; }
}
