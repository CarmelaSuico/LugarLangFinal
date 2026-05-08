package com.usc.lugarlangfinal.models;

import com.google.firebase.database.PropertyName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogTrip {
    private String driverName;
    private String RouteCode;
    private String Terminal1;
    private String Terminal2;
    private String departureTime;
    private long tripEndTime; // From your screenshot
    private String employeeId; // We will set this manually during data fetch

    private String VehicleCode;
    private String franchise;

    private String AssignedTransport;

    public LogTrip() {}

    @PropertyName("AssignedTransport")
    public String getAssignedTransport() {
        return AssignedTransport;
    }

    @PropertyName("AssignedTransport")
    public void setAssignedTransport(String assignedTransport) {
        this.AssignedTransport = assignedTransport;
    }

    public String getFranchise() { return franchise; }
    public void setFranchise(String franchise) { this.franchise = franchise; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    @PropertyName("RouteCode")
    public String getRouteCode() { return RouteCode; }
    @PropertyName("RouteCode")
    public void setRouteCode(String routeCode) { this.RouteCode = routeCode; }

    @PropertyName("Terminal1")
    public String getTerminal1() { return Terminal1; }
    @PropertyName("Terminal1")
    public void setTerminal1(String terminal1) { this.Terminal1 = terminal1; }

    @PropertyName("Terminal2")
    public String getTerminal2() { return Terminal2; }
    @PropertyName("Terminal2")
    public void setTerminal2(String terminal2) { this.Terminal2 = terminal2; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    @PropertyName("VehicleCode")
    public String getVehicleCode() { return VehicleCode; }

    @PropertyName("VehicleCode")
    public void setVehicleCode(String vehicleCode) { this.VehicleCode = vehicleCode; }

    public long getTripEndTime() { return tripEndTime; }
    public void setTripEndTime(long tripEndTime) { this.tripEndTime = tripEndTime; }

    // Helper to turn that long timestamp into a readable date
    public String getFormattedEndTime() {
        if (tripEndTime == 0) return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(tripEndTime));
    }
}