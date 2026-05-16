package com.clinic.shared.dto;

import java.io.Serializable;

public class ReceptionIntakeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String fullName;
    private int age;
    private String gender;
    private String phone;
    private String address;
    private String doctorId;

    public ReceptionIntakeRequest() {
    }

    public ReceptionIntakeRequest(String code, String fullName, int age, String gender, String phone, String address, String doctorId) {
        this.code = code;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.phone = phone;
        this.address = address;
        this.doctorId = doctorId;
    }

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
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
}
