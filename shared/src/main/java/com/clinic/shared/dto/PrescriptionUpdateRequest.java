package com.clinic.shared.dto;

import com.clinic.shared.model.PrescriptionItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String prescriptionId;
    private List<PrescriptionItem> items = new ArrayList<>();
    private String status;

    public PrescriptionUpdateRequest() {
    }

    public PrescriptionUpdateRequest(String prescriptionId, List<PrescriptionItem> items, String status) {
        this.prescriptionId = prescriptionId;
        this.items = items;
        this.status = status;
    }

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }
    public List<PrescriptionItem> getItems() { return items; }
    public void setItems(List<PrescriptionItem> items) { this.items = items; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
