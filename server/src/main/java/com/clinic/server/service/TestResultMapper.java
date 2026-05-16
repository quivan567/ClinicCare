package com.clinic.server.service;

import com.clinic.server.entity.TestResultEntity;
import com.clinic.shared.dto.TestResultRequest;
import com.clinic.shared.model.TestResult;

public final class TestResultMapper {
    private TestResultMapper() {
    }

    public static TestResult toModel(TestResultEntity e) {
        if (e == null) return null;
        TestResult m = new TestResult();
        m.setId(e.getId());
        m.setPatientId(e.getPatientId());
        m.setDoctorId(e.getDoctorId());
        m.setTestingStaffId(e.getTestingStaffId());
        m.setTestName(e.getTestName());
        m.setResult(e.getResult());
        m.setNotes(e.getNotes());
        m.setCreatedAt(e.getCreatedAt());
        return m;
    }

    public static TestResultEntity toEntity(TestResultRequest r) {
        TestResultEntity e = new TestResultEntity();
        e.setPatientId(r.getPatientId());
        e.setDoctorId(r.getDoctorId());
        e.setTestingStaffId(r.getTestingStaffId());
        e.setTestName(r.getTestName());
        e.setResult(r.getResult());
        e.setNotes(r.getNotes());
        return e;
    }
}
