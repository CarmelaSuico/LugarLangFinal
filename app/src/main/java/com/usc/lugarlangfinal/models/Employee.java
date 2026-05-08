package com.usc.lugarlangfinal.models;

import com.google.firebase.database.PropertyName;

public class Employee {
    private String id, name, role, AssignedUnit, Franchise, status;
    private String email, licenseNumber, contactNumber, address, password, authUID;

    public Employee() {}

    // CRITICAL FIX: Changed parameter to Object to prevent Long-to-String crash
    public String getId() { return id; }
    public void setId(Object id) {
        this.id = String.valueOf(id);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @PropertyName("AssignedUnit")
    public String getAssignedUnit() { return AssignedUnit; }
    @PropertyName("AssignedUnit")
    public void setAssignedUnit(String assignedUnit) { this.AssignedUnit = assignedUnit; }

    @PropertyName("Franchise")
    public String getFranchise() { return Franchise; }
    @PropertyName("Franchise")
    public void setFranchise(String franchise) { this.Franchise = franchise; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAuthUID() { return authUID; }
    public void setAuthUID(String authUID) { this.authUID = authUID; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @PropertyName("status")
    public String getStatus() { return status; }
    @PropertyName("status")
    public void setStatus(String status) { this.status = status; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}