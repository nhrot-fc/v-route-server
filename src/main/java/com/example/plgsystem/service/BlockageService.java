package com.example.plgsystem.service;

import com.example.plgsystem.model.Blockage;
import com.example.plgsystem.repository.BlockageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BlockageService {

    private final BlockageRepository blockageRepository;

    public BlockageService(BlockageRepository blockageRepository) {
        this.blockageRepository = blockageRepository;
    }

    /**
     * Guarda un bloqueo (crear o actualizar)
     */
    @Transactional
    public Blockage save(Blockage blockage) {
        return blockageRepository.save(blockage);
    }

    /**
     * Busca un bloqueo por su ID
     */
    public Optional<Blockage> findById(Long id) {
        return blockageRepository.findById(id);
    }

    /**
     * Obtiene todos los bloqueos
     */
    public List<Blockage> findAll() {
        return blockageRepository.findAll();
    }

    /**
     * Elimina un bloqueo por su ID
     */
    @Transactional
    public void deleteById(Long id) {
        blockageRepository.deleteById(id);
    }

    /**
     * Obtiene bloqueos activos en un momento específico
     */
    public List<Blockage> findByActiveAtDateTime(LocalDateTime dateTime) {
        return blockageRepository.findByActiveAtDateTime(dateTime);
    }

    /**
     * Obtiene bloqueos en un rango de tiempo
     */
    public List<Blockage> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return blockageRepository.findByStartTimeGreaterThanEqualAndEndTimeLessThanEqual(startTime, endTime);
    }

    /**
     * Carga bloqueos para un mes específico desde el archivo correspondiente
     *
     * @param year Año
     * @param month Mes (1-12)
     * @param baseDirectory Directorio base donde se encuentran los archivos
     * @return Lista de bloqueos cargados
     */
    @Transactional
    public List<Blockage> loadBlockagesForMonth(int year, int month, String baseDirectory) {
        // Construir la ruta al archivo
        String fileName = String.format("%04d%02d.bloqueadas", year, month);
        Path filePath = Paths.get(baseDirectory, fileName);

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("El archivo de bloqueos no existe: " + filePath);
        }

        // Crear fecha base: primer día del mes y año especificados
        LocalDateTime baseDate = LocalDateTime.of(year, month, 1, 0, 0);
        List<Blockage> loadedBlockages = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
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
                    Blockage savedBlockage = save(blockage);
                    loadedBlockages.add(savedBlockage);
                } catch (Exception e) {
                    // Aquí podrías usar un logger para registrar el error
                    System.err.println("Error al procesar la línea " + lineNumber + ": " + e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo de bloqueos", e);
        }

        return loadedBlockages;
    }
}
