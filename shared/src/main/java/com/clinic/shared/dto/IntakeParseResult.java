package com.clinic.shared.dto;

import java.io.Serializable;

public class IntakeParseResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private String fullName;
    private Integer age;
    private String gender;
    private String phone;
    private String address;

    public IntakeParseResult() {
    }

    public static IntakeParseResult ok(String fullName, Integer age, String gender, String phone, String address) {
        IntakeParseResult result = new IntakeParseResult();
        result.success = true;
        result.message = "Đã điền phiếu từ mô tả. Hãy kiểm tra rồi bấm Tiếp nhận.";
        result.fullName = fullName;
        result.age = age;
        result.gender = gender;
        result.phone = phone;
        result.address = address;
        return result;
    }

    public static IntakeParseResult fail(String message) {
        IntakeParseResult result = new IntakeParseResult();
        result.success = false;
        result.message = message;
        return result;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
