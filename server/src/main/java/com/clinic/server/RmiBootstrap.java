package com.clinic.server;

import com.clinic.server.service.ClinicServiceImpl;
import org.springframework.stereotype.Component;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

@Component
public class RmiBootstrap {
    private final ClinicServiceImpl service;
    private boolean started = false;

    public RmiBootstrap(ClinicServiceImpl service) {
        this.service = service;
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        try {
            try {
                LocateRegistry.createRegistry(1099);
            } catch (Exception ignored) {
                // Registry may already exist
            }
            Naming.rebind("ClinicService", service);
            System.out.println("RMI Service started on port 1099");
            started = true;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start RMI service", e);
        }
    }
}
