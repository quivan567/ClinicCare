package com.clinic.server.service;

import com.clinic.server.entity.UserEntity;
import com.clinic.shared.dto.UserRequest;
import com.clinic.shared.model.User;

public final class UserMapper {
    private UserMapper() {
    }

    public static User toModel(UserEntity e) {
        if (e == null) {
            return null;
        }
        User u = new User();
        u.setId(e.getId());
        u.setUsername(e.getUsername());
        u.setRole(e.getRole());
        u.setFullName(e.getFullName());
        u.setActive(e.isActive());
        return u;
    }

    public static UserEntity toEntity(UserRequest r) {
        UserEntity e = new UserEntity();
        e.setId(r.getId());
        e.setUsername(r.getUsername());
        e.setPassword(r.getPassword());
        e.setRole(r.getRole());
        e.setFullName(r.getFullName());
        e.setActive(r.isActive());
        return e;
    }
}
