package com.example.plgsystem.util;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.service.BlockageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para cargar bloqueos desde archivos
 */
@Component
public class BlockageFileLoader {
    private static final Logger logger = LoggerFactory.getLogger(BlockageFileLoader.class);
    private final BlockageService blockageService;

    public BlockageFileLoader(BlockageService blockageService) {
        this.blockageService = blockageService;
    }

    /**
     * Carga bloqueos desde un archivo y los guarda en la base de datos
     * 
     * @param filePath Ruta al archivo de bloqueos
     * @param year Año para la fecha base
     * @param month Mes para la fecha base (1-12)
     * @return Lista de bloqueos cargados
     */
    public List<Blockage> loadBlockagesFromFile(String filePath, int year, int month) {
        logger.info("Cargando bloqueos desde archivo: {}", filePath);
        
        // Crear fecha base: primer día del mes y año especificados
        LocalDateTime baseDate = LocalDateTime.of(year, month, 1, 0, 0);
        List<Blockage> loadedBlockages = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Saltar líneas vacías o comentarios
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                try {
                    Blockage blockage = Blockage.fromFileFormat(line, baseDate);
                    Blockage savedBlockage = blockageService.save(blockage);
                    loadedBlockages.add(savedBlockage);
                    
                    logger.debug("Bloqueo cargado: {} hasta {}", 
                            blockage.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            blockage.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e) {
                    logger.error("Error al procesar la línea {} del archivo {}: {}", 
                            lineNumber, filePath, e.getMessage(), e);
                }
            }
            
            logger.info("Cargados {} bloqueos desde el archivo {}", loadedBlockages.size(), filePath);
            
        } catch (IOException e) {
            logger.error("Error al leer el archivo de bloqueos {}: {}", filePath, e.getMessage(), e);
            throw new RuntimeException("Error al cargar el archivo de bloqueos", e);
        }
        
        return loadedBlockages;
    }
    
    /**
     * Construye el nombre de archivo para los bloqueos según el formato en Constants.STREET_CLOSURE_FILE_BASE_NAME
     * 
     * @param year Año
     * @param month Mes (1-12)
     * @return Nombre de archivo formateado
     */
    public static String buildBlockageFileName(int year, int month) {
        return String.format("%04d%02d.bloqueadas", year, month);
    }
}
