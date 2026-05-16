package com.clinic.shared.model;

import java.io.Serializable;

public class PrescriptionItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String medicineName;
    private String dosage;
    private String instructions;

    public PrescriptionItem() {
    }

    public PrescriptionItem(String medicineName, String dosage, String instructions) {
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.instructions = instructions;
    }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
}
