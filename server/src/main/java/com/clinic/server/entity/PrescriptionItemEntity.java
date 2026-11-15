package com.clinic.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "prescription_items")
public class PrescriptionItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private PrescriptionEntity prescription;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Column(name = "medicine_name", nullable = false, length = 150)
    private String medicineName;

    @Column(length = 150)
    private String dosage;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public PrescriptionEntity getPrescription() { return prescription; }
    public void setPrescription(PrescriptionEntity prescription) { this.prescription = prescription; }
    public int getLineNo() { return lineNo; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
