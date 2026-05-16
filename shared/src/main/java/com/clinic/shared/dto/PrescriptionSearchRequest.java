package com.clinic.shared.dto;

import java.io.Serializable;

public class PrescriptionSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String patientCode;
    private String doctorKeyword;
    private String status;

    public PrescriptionSearchRequest() {
    }

    public PrescriptionSearchRequest(String patientCode, String doctorKeyword, String status) {
        this.patientCode = patientCode;
        this.doctorKeyword = doctorKeyword;
        this.status = status;
    }

    public String getPatientCode() { return patientCode; }
    public void setPatientCode(String patientCode) { this.patientCode = patientCode; }
    public String getDoctorKeyword() { return doctorKeyword; }
    public void setDoctorKeyword(String doctorKeyword) { this.doctorKeyword = doctorKeyword; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
