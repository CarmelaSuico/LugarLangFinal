package com.usc.lugarlangfinal.models;

import com.google.firebase.database.PropertyName;

public class Employee {
    private String id, name, role, assignedUnit, franchise, status;
    private String email, licenseNumber, contactNumber, address, password;

    // Default constructor is REQUIRED for Firebase
    public Employee() {}

    // 11-Argument Constructor (Includes Password)
    public Employee(String id, String name, String role, String assignedUnit, String franchise,
                    String email, String licenseNumber, String contactNumber, String address,
                    String status, String password) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.assignedUnit = assignedUnit;
        this.franchise = franchise;
        this.email = email;
        this.licenseNumber = licenseNumber;
        this.contactNumber = contactNumber;
        this.address = address;
        this.status = status;
        this.password = password;
    }

    // --- GETTERS AND SETTERS WITH PROPERTY NAME TAGS ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @PropertyName("AssignedUnit")
    public String getAssignedUnit() { return assignedUnit; }
    @PropertyName("AssignedUnit")
    public void setAssignedUnit(String assignedUnit) { this.assignedUnit = assignedUnit; }

    @PropertyName("Franchise")
    public String getFranchise() { return franchise; }
    @PropertyName("Franchise")
    public void setFranchise(String franchise) { this.franchise = franchise; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}