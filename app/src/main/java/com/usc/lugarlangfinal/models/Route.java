package com.usc.lugarlangfinal.models;

import com.google.firebase.database.PropertyName;

public class Route {
    private String code, terminal1, terminal2, stops, assignedtransport, status;
    private String t1_coords, t2_coords, stops_cords;
    private double distance, basefar, addfareperband;
    private int distancebands;

    public Route() {}

    public Route(String code, String terminal1, String terminal2, String stops, String assignedtransport, String status, String t1_coords, String t2_coords, String stops_cords, double distance, double basefar, double addfareperband, int distancebands){
        this.code = code;
        this.terminal1 = terminal1;
        this.terminal2 = terminal2;
        this.stops = stops;
        this.assignedtransport = assignedtransport;
        this.status = status;
        this.t1_coords = t1_coords;
        this.t2_coords = t2_coords;
        this.stops_cords = stops_cords;
        this.distance = distance;
        this.basefar = basefar;
        this.addfareperband = addfareperband;
        this.distancebands = distancebands;
    }

    @PropertyName("RouteCode")
    public String getCode() {
        return code;
    }

    @PropertyName("RouteCode")
    public void setCode(String code) {
        this.code = code;
    }

    @PropertyName("Terminal1")
    public String getTerminal1() {
        return terminal1;
    }

    @PropertyName("Terminal1")
    public void setTerminal1(String terminal1) {
        this.terminal1 = terminal1;
    }

    @PropertyName("Terminal2")
    public String getTerminal2() {
        return terminal2;
    }

    @PropertyName("Terminal2")
    public void setTerminal2(String terminal2) {
        this.terminal2 = terminal2;
    }


    @PropertyName("Stops")
    public String getStops() {
        return stops;
    }

    @PropertyName("Stops")
    public void setStops(String stops) {
        this.stops = stops;
    }

    @PropertyName("AssignedTransport")
    public String getAssignedtransport() {
        return assignedtransport;
    }

    @PropertyName("AssignedTransport")
    public void setAssignedtransport(String assignedtransport) {
        this.assignedtransport = assignedtransport;
    }

    @PropertyName("Status")
    public String getStatus() {
        return status;
    }

    @PropertyName("Status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("T1_Coords")
    public String getT1_coords() {
        return t1_coords;
    }

    @PropertyName("T1_Coords")
    public void setT1_coords(String t1_coords) {
        this.t1_coords = t1_coords;
    }

    @PropertyName("T2_Coords")
    public String getT2_coords() {
        return t2_coords;
    }

    @PropertyName("T2_Coords")
    public void setT2_coords(String t2_coords) {
        this.t2_coords = t2_coords;
    }

    @PropertyName("Stop_Coords")
    public String getStops_cords() {
        return stops_cords;
    }

    @PropertyName("Stop_Coords")
    public void setStops_cords(String stops_cords) {
        this.stops_cords = stops_cords;
    }

    @PropertyName("Distance")
    public double getDistance() {
        return distance;
    }

    @PropertyName("Distance")
    public void setDistance(double distance) {
        this.distance = distance;
    }

    @PropertyName("BaseFare")
    public double getBasefar() {
        return basefar;
    }

    @PropertyName("BaseFare")
    public void setBasefar(double basefar) {
        this.basefar = basefar;
    }

    @PropertyName("AddFarePerBand")
    public double getAddfareperband() {
        return addfareperband;
    }

    @PropertyName("AddFarePerBand")
    public void setAddfareperband(double addfareperband) {
        this.addfareperband = addfareperband;
    }

    @PropertyName("DistanceBands")
    public int getDistancebands() {
        return distancebands;
    }

    @PropertyName("DistanceBands")
    public void setDistancebands(int distancebands) {
        this.distancebands = distancebands;
    }
}
