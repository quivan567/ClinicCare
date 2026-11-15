package com.clinic.shared.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Prescription implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String patientId;
    private String patientCode;
    private String patientName;
    private String doctorId;
    private String doctorCode;
    private String doctorName;
    private List<PrescriptionItem> items = new ArrayList<>();
    private String status;
    private LocalDateTime createdAt;

    public Prescription() {
    }

    public Prescription(String id, String patientId, String doctorId, List<PrescriptionItem> items, String status, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.items = items;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getPatientCode() { return patientCode; }
    public void setPatientCode(String patientCode) { this.patientCode = patientCode; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public String getDoctorCode() { return doctorCode; }
    public void setDoctorCode(String doctorCode) { this.doctorCode = doctorCode; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public List<PrescriptionItem> getItems() { return items; }
    public void setItems(List<PrescriptionItem> items) { this.items = items; }
    public String getMedicineName() { return items == null || items.isEmpty() ? "" : items.get(0).getMedicineName(); }
    public void setMedicineName(String medicineName) {
        ensureFirstItem();
        items.get(0).setMedicineName(medicineName);
    }
    public String getDosage() { return items == null || items.isEmpty() ? "" : items.get(0).getDosage(); }
    public void setDosage(String dosage) {
        ensureFirstItem();
        items.get(0).setDosage(dosage);
    }
    public String getInstructions() { return items == null || items.isEmpty() ? "" : items.get(0).getInstructions(); }
    public void setInstructions(String instructions) {
        ensureFirstItem();
        items.get(0).setInstructions(instructions);
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    private void ensureFirstItem() {
        if (items == null) {
            items = new ArrayList<>();
        }
        if (items.isEmpty()) {
            items.add(new PrescriptionItem());
        }
    }
}
