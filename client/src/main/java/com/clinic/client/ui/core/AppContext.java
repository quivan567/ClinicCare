package com.clinic.client.ui.core;

import com.clinic.shared.model.User;
import com.clinic.shared.rmi.ClinicService;

public class AppContext {
    private final ClinicService service;
    private final Runnable logoutHandler;
    private User currentUser;

    public AppContext(ClinicService service, Runnable logoutHandler) {
        this.service = service;
        this.logoutHandler = logoutHandler;
    }

    public ClinicService getService() {
        return service;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void clearCurrentUser() {
        this.currentUser = null;
    }

    public void logout() {
        logoutHandler.run();
    }
}
