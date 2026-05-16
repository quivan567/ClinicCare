package com.clinic.shared.dto;

import com.clinic.shared.model.PrescriptionItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CreatePrescriptionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String patientId;
    private String doctorId;
    private List<PrescriptionItem> items = new ArrayList<>();

    public CreatePrescriptionRequest() {
    }

    public CreatePrescriptionRequest(String patientId, String doctorId, List<PrescriptionItem> items) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.items = items;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public List<PrescriptionItem> getItems() { return items; }
    public void setItems(List<PrescriptionItem> items) { this.items = items; }
}
