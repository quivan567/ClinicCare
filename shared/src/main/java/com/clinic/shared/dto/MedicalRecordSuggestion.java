package com.clinic.shared.dto;

import java.io.Serializable;

public class MedicalRecordSuggestion implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private String diagnosis;
    private String notes;
    private boolean requiresTest;

    public MedicalRecordSuggestion() {
    }

    public static MedicalRecordSuggestion ok(String diagnosis, String notes, boolean requiresTest) {
        MedicalRecordSuggestion suggestion = new MedicalRecordSuggestion();
        suggestion.success = true;
        suggestion.message = "Đã điền gợi ý. Hãy kiểm tra rồi bấm Lưu hồ sơ.";
        suggestion.diagnosis = diagnosis;
        suggestion.notes = notes;
        suggestion.requiresTest = requiresTest;
        return suggestion;
    }

    public static MedicalRecordSuggestion fail(String message) {
        MedicalRecordSuggestion suggestion = new MedicalRecordSuggestion();
        suggestion.success = false;
        suggestion.message = message;
        return suggestion;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isRequiresTest() { return requiresTest; }
    public void setRequiresTest(boolean requiresTest) { this.requiresTest = requiresTest; }
}
