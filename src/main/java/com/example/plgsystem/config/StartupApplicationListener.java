package com.example.plgsystem.config;

import com.example.plgsystem.service.DatabaseInitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Listener that performs actions when the application starts up.
 * This ensures the database is properly initialized before any services are used.
 */
@Component
public class StartupApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StartupApplicationListener.class);
    private final DatabaseInitializationService databaseInitializationService;
    private boolean initialized = false;

    public StartupApplicationListener(DatabaseInitializationService databaseInitializationService) {
        this.databaseInitializationService = databaseInitializationService;
    }

    @Override
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        if (!initialized) {
            logger.info("Application context refreshed, initializing database");
            try {
                databaseInitializationService.initializeDatabase();
                initialized = true;
                logger.info("Database initialization complete");
            } catch (Exception e) {
                logger.error("Failed to initialize database: {}", e.getMessage(), e);
                // We don't rethrow the exception as we want the application to start
                // even if database initialization fails
            }
        }
    }
} 