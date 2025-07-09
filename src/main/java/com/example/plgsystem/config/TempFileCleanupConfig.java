package com.example.plgsystem.config;

import com.example.plgsystem.util.FileUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para limpiar archivos temporales al iniciar la aplicación
 */
@Configuration
public class TempFileCleanupConfig {
    private static final Logger logger = LoggerFactory.getLogger(TempFileCleanupConfig.class);

    /**
     * Método ejecutado después de la inicialización del bean para limpiar archivos temporales
     */
    @PostConstruct
    public void cleanupTemporaryFiles() {
        logger.info("Limpiando archivos temporales al iniciar la aplicación");
        FileUtils.cleanupTempFiles();
        logger.info("Limpieza de archivos temporales completada");
    }
} 