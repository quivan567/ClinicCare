package com.clinic.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.clinic.server")
@EnableMongoRepositories(basePackages = "com.clinic.server.repository")
public class ClinicServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClinicServerApplication.class, args);
    }

    @Bean
    CommandLineRunner startRmi(RmiBootstrap bootstrap) {
        return args -> bootstrap.start();
    }
}
