package com.clinic.shared.model;

public final class PatientStatus {
    public static final String WAITING = "Chờ xử lý";
    public static final String ASSIGNED = "Đã phân công";
    public static final String EXAMINING = "Đang khám";
    public static final String WAITING_FOR_TEST = "Chờ xét nghiệm";
    public static final String HAS_TEST_RESULT = "Đã có kết quả xét nghiệm";
    public static final String EXAMINED = "Đã khám";
    public static final String WAITING_FOR_PRESCRIPTION = "Chờ kê đơn";
    public static final String PRESCRIBED = "Đã kê đơn";
    public static final String COMPLETED = "Đã hoàn tất";

    private PatientStatus() {
    }
}
