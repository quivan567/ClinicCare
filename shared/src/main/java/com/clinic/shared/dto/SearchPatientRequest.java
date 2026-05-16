package com.clinic.shared.dto;

import java.io.Serializable;

public class SearchPatientRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;

    public SearchPatientRequest() {
    }

    public SearchPatientRequest(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
