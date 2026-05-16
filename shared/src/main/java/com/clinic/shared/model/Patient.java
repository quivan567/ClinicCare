package com.clinic.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Patient implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String fullName;
    private int age;
    private String gender;
    private String phone;
    private String address;
    private String assignedDoctorId;
    private String status;
    private LocalDateTime lastVisitAt;

    public Patient() {
    }

    public Patient(String id, String code, String fullName, int age, String gender, String phone, String address, String assignedDoctorId, String status, LocalDateTime lastVisitAt) {
        this.id = id;
        this.code = code;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.address = address;
        this.assignedDoctorId = assignedDoctorId;
        this.status = status;
        this.lastVisitAt = lastVisitAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getAssignedDoctorId() { return assignedDoctorId; }
    public void setAssignedDoctorId(String assignedDoctorId) { this.assignedDoctorId = assignedDoctorId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getLastVisitAt() { return lastVisitAt; }
    public void setLastVisitAt(LocalDateTime lastVisitAt) { this.lastVisitAt = lastVisitAt; }
}
