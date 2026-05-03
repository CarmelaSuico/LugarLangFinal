package com.usc.lugarlangfinal.models;

import com.google.firebase.database.PropertyName;

public class Route {
    private String routeCode, terminal1, terminal2, stops, assignedTransport, status, company;
    private double baseFare, additionalFarePerBand, distance;
    private int distanceBands;
    private String t1_Coords, t2_Coords, stops_Coords;

    public Route() {}

    @PropertyName("RouteCode")
    public String getRouteCode() { return routeCode; }
    @PropertyName("RouteCode")
    public void setRouteCode(String routeCode) { this.routeCode = routeCode; }

    @PropertyName("Terminal1")
    public String getTerminal1() { return terminal1; }
    @PropertyName("Terminal1")
    public void setTerminal1(String terminal1) { this.terminal1 = terminal1; }

    @PropertyName("Terminal2")
    public String getTerminal2() { return terminal2; }
    @PropertyName("Terminal2")
    public void setTerminal2(String terminal2) { this.terminal2 = terminal2; }

    @PropertyName("Stops")
    public String getStops() { return stops; }
    @PropertyName("Stops")
    public void setStops(String stops) { this.stops = stops; }

    @PropertyName("BaseFare")
    public double getBaseFare() { return baseFare; }
    @PropertyName("BaseFare")
    public void setBaseFare(double baseFare) { this.baseFare = baseFare; }

    @PropertyName("DistanceBands")
    public int getDistanceBands() { return distanceBands; }
    @PropertyName("DistanceBands")
    public void setDistanceBands(int distanceBands) { this.distanceBands = distanceBands; }

    @PropertyName("AdditionalFarePerBand")
    public double getAdditionalFarePerBand() { return additionalFarePerBand; }
    @PropertyName("AdditionalFarePerBand")
    public void setAdditionalFarePerBand(double additionalFarePerBand) { this.additionalFarePerBand = additionalFarePerBand; }

    @PropertyName("AssignedTransport")
    public String getAssignedTransport() { return assignedTransport; }
    @PropertyName("AssignedTransport")
    public void setAssignedTransport(String assignedTransport) { this.assignedTransport = assignedTransport; }

    @PropertyName("Status")
    public String getStatus() { return status; }
    @PropertyName("Status")
    public void setStatus(String status) { this.status = status; }

    @PropertyName("Company")
    public String getCompany() { return company; }
    @PropertyName("Company")
    public void setCompany(String company) { this.company = company; }

    @PropertyName("Distance")
    public double getDistance() { return distance; }
    @PropertyName("Distance")
    public void setDistance(double distance) { this.distance = distance; }

    @PropertyName("T1_Coords")
    public String getT1_Coords() { return t1_Coords; }
    @PropertyName("T1_Coords")
    public void setT1_Coords(String t1_Coords) { this.t1_Coords = t1_Coords; }

    @PropertyName("T2_Coords")
    public String getT2_Coords() { return t2_Coords; }
    @PropertyName("T2_Coords")
    public void setT2_Coords(String t2_Coords) { this.t2_Coords = t2_Coords; }

    @PropertyName("Stop_Coords")
    public String getStops_Coords() { return stops_Coords; }
    @PropertyName("Stop_Coords")
    public void setStops_Coords(String stops_Coords) { this.stops_Coords = stops_Coords; }
}
