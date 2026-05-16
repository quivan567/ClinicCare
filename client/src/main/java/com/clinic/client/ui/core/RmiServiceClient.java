package com.clinic.client.ui.core;

import com.clinic.shared.rmi.ClinicService;

import java.rmi.Naming;

public final class RmiServiceClient {
    private RmiServiceClient() {
    }

    public static ClinicService connect(String serviceUrl) throws Exception {
        return (ClinicService) Naming.lookup(serviceUrl);
    }
}
