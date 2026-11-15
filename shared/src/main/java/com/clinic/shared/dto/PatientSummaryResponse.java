package com.clinic.shared.dto;

import java.io.Serializable;

public class PatientSummaryResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String summary;
    private String message;

    public PatientSummaryResponse() {
    }

    public static PatientSummaryResponse ok(String summary) {
        PatientSummaryResponse response = new PatientSummaryResponse();
        response.success = true;
        response.summary = summary;
        response.message = "Đã tạo tóm tắt hồ sơ";
        return response;
    }

    public static PatientSummaryResponse fail(String message) {
        PatientSummaryResponse response = new PatientSummaryResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
