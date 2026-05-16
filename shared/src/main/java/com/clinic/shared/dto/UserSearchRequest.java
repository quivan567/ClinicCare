package com.clinic.shared.dto;

import java.io.Serializable;

public class UserSearchRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String keyword;

    public UserSearchRequest() {
    }

    public UserSearchRequest(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
