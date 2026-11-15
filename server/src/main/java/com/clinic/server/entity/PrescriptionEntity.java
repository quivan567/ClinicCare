package com.clinic.server.entity;

import com.clinic.shared.model.PrescriptionItem;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prescriptions", indexes = {
        @Index(name = "idx_prescriptions_patient", columnList = "patient_id"),
        @Index(name = "idx_prescriptions_doctor", columnList = "doctor_id"),
        @Index(name = "idx_prescriptions_status", columnList = "status")
})
public class PrescriptionEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "patient_id", nullable = false, length = 36)
    private String patientId;

    @Column(name = "doctor_id", nullable = false, length = 36)
    private String doctorId;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @OrderBy("lineNo")
    private List<PrescriptionItemEntity> items = new ArrayList<>();

    @Column(length = 50)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void assignId() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }

    public void replaceItems(List<PrescriptionItem> source) {
        items.clear();
        if (source == null) {
            return;
        }
        int lineNo = 1;
        for (PrescriptionItem item : source) {
            if (item == null) {
                continue;
            }
            PrescriptionItemEntity row = new PrescriptionItemEntity();
            row.setPrescription(this);
            row.setLineNo(lineNo++);
            row.setMedicineName(item.getMedicineName());
            row.setDosage(item.getDosage());
            row.setInstructions(item.getInstructions());
            items.add(row);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    public List<PrescriptionItemEntity> getItems() { return items; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
