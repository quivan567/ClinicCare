package com.clinic.client.ui.core;

import com.clinic.shared.model.User;
import com.clinic.shared.rmi.ClinicService;

public class AppContext {
    private final ClinicService service;
    private final Runnable logoutHandler;
    private User currentUser;
    private long suppressRealtimeUntil;

    public void reloadAfterLocalChange(Runnable reload) {
        reload.run();
        suppressRealtimeUntil = System.currentTimeMillis() + 1200;
    }

    public boolean isRealtimeSuppressed() {
        return System.currentTimeMillis() < suppressRealtimeUntil;
    }

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
