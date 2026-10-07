package com.smartomni.jobs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JobsApplication {
    public static void main(String[] args) {
        String token = System.getenv("INTERNAL_JOB_TOKEN");
        if (token == null || token.length() < 32) {
            throw new IllegalStateException("INTERNAL_JOB_TOKEN must be at least 32 characters");
        }
        SpringApplication.run(JobsApplication.class, args);
    }
}
