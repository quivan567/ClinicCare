package com.clinic.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

@SpringBootApplication(scanBasePackages = "com.clinic.server")
public class ClinicServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClinicServerApplication.class, args);
    }

    @Bean
    @Order(2)
    CommandLineRunner startRmi(RmiBootstrap bootstrap) {
        return args -> bootstrap.start();
    }
}
