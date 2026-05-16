package com.clinic.shared.dto;

public final class RealtimeEventType {
    public static final String PATIENT_CHANGED = "PATIENT_CHANGED";
    public static final String PRESCRIPTION_CHANGED = "PRESCRIPTION_CHANGED";
    public static final String MEDICAL_RECORD_CHANGED = "MEDICAL_RECORD_CHANGED";
    public static final String TEST_RESULT_CHANGED = "TEST_RESULT_CHANGED";
    public static final String USER_CHANGED = "USER_CHANGED";

    private RealtimeEventType() {
    }
}
