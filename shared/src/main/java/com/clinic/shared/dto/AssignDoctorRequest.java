package com.clinic.shared.dto;

import java.io.Serializable;

public class AssignDoctorRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String patientId;
    private String doctorId;

    public AssignDoctorRequest() {
    }

    public AssignDoctorRequest(String patientId, String doctorId) {
        this.patientId = patientId;
        this.doctorId = doctorId;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
}
