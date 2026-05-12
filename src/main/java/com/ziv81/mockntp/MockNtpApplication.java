package com.ziv81.mockntp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MockNtpApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockNtpApplication.class, args);
    }
}
