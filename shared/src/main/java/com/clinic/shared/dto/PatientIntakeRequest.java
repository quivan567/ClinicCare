package com.clinic.shared.dto;

import com.clinic.shared.model.Patient;

import java.io.Serializable;

public class PatientIntakeRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;
    private String doctorId;
    private Patient patient;

    public PatientIntakeRequest() {
    }

    public PatientIntakeRequest(String keyword, String doctorId, Patient patient) {
        this.keyword = keyword;
        this.doctorId = doctorId;
        this.patient = patient;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
}
