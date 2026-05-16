package com.clinic.shared.dto;

import java.io.Serializable;

public class DispensePrescriptionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String prescriptionId;
    private String pharmacistId;

    public DispensePrescriptionRequest() {
    }

    public DispensePrescriptionRequest(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public DispensePrescriptionRequest(String prescriptionId, String pharmacistId) {
        this.prescriptionId = prescriptionId;
        this.pharmacistId = pharmacistId;
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getPharmacistId() {
        return pharmacistId;
    }

    public void setPharmacistId(String pharmacistId) {
        this.pharmacistId = pharmacistId;
    }
}
