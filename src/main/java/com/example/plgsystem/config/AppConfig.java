package com.example.plgsystem.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.example.plgsystem.demo.AlgorithmExperiment;

@Configuration
public class AppConfig {

    @Bean
    @Profile("algorithm-experiment")
    public CommandLineRunner algorithmExperimentRunner() {
        return args -> {
            System.out.println("Starting algorithm experiment...");
            String[] experimentArgs = {};
            AlgorithmExperiment.main(experimentArgs);
            System.exit(0); // Exit after experiment completes
        };
    }
}
