package com.example.plgsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlgSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlgSystemApplication.class, args);
    }

}
