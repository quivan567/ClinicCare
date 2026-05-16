package com.clinic.shared.dto;

import java.io.Serializable;

public class AcceptPatientRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String patientCode;
    private String doctorId;

    public AcceptPatientRequest() {
    }

    public AcceptPatientRequest(String patientCode, String doctorId) {
        this.patientCode = patientCode;
        this.doctorId = doctorId;
    }

    public String getPatientCode() { return patientCode; }
    public void setPatientCode(String patientCode) { this.patientCode = patientCode; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
}
