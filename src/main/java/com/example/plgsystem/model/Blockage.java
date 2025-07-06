package com.example.plgsystem.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "blockages")
@Getter
@NoArgsConstructor
public class Blockage implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    // Almacenamos solo los puntos que definen los tramos como una cadena
    // serializada
    // Formato: "x1,y1;x2,y2;...;xn,yn"
    @Column(nullable = false, length = 4000)
    private String linePoints;

    // Campo transitorio, no se guarda en la base de datos
    @Transient
    private List<Position> lines;

    /**
     * Constructor principal para crear un nuevo bloqueo
     */
    public Blockage(LocalDateTime startTime, LocalDateTime endTime, List<Position> blockageLines) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.lines = new ArrayList<>(blockageLines);
        // Serializamos los puntos para almacenamiento
        this.linePoints = serializePositions(blockageLines);
    }

    /**
     * Serializa una lista de posiciones en un formato de cadena para almacenamiento
     */
    private String serializePositions(List<Position> positions) {
        return positions.stream()
                .map(pos -> pos.getX() + "," + pos.getY())
                .collect(Collectors.joining(";"));
    }

    /**
     * Deserializa una cadena de posiciones de nuevo a objetos Position
     */
    private List<Position> deserializePositions(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return Collections.emptyList();
        }

        String[] points = serialized.split(";");
        List<Position> result = new ArrayList<>(points.length);

        for (String point : points) {
            String[] coordinates = point.split(",");
            if (coordinates.length == 2) {
                try {
                    int x = Integer.parseInt(coordinates[0]);
                    int y = Integer.parseInt(coordinates[1]);
                    result.add(new Position(x, y));
                } catch (NumberFormatException e) {
                    // Log error or handle invalid data
                }
            }
        }

        return result;
    }

    /**
     * Método llamado después de cargar la entidad desde la base de datos
     */
    @PostLoad
    private void postLoad() {
        // Recreamos la lista de posiciones a partir de la cadena almacenada
        this.lines = deserializePositions(this.linePoints);
    }

    /**
     * Retorna una lista inmutable de las posiciones que definen los tramos
     */
    public List<Position> getLines() {
        // Aseguramos que lines esté inicializado
        if (lines == null && linePoints != null) {
            lines = deserializePositions(linePoints);
        }
        return Collections.unmodifiableList(lines);
    }

    /**
     * Verifica si el bloqueo está activo en un momento dado
     */
    public boolean isActiveAt(LocalDateTime dateTime) {
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime);
    }

    /**
     * Verifica si una posición está bloqueada por alguno de los tramos del bloqueo
     */
    public boolean posicionEstaBloqueada(Position posicion) {
        List<Position> lineas = getLines();
        for (int i = 0; i < lineas.size() - 1; i++) {
            Position p1 = lineas.get(i);
            Position p2 = lineas.get(i + 1);

            // Tramo vertical (mismo X)
            if (p1.getX() == p2.getX() && p1.getX() == posicion.getX()) {
                int minY = Math.min(p1.getY(), p2.getY());
                int maxY = Math.max(p1.getY(), p2.getY());
                if (minY <= posicion.getY() && posicion.getY() <= maxY) {
                    return true;
                }
            } 
            // Tramo horizontal (mismo Y)
            else if (p1.getY() == p2.getY() && p1.getY() == posicion.getY()) {
                int minX = Math.min(p1.getX(), p2.getX());
                int maxX = Math.max(p1.getX(), p2.getX());
                if (minX <= posicion.getX() && posicion.getX() <= maxX) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Parsea una línea de texto en formato de archivo de bloqueos y crea una
     * instancia de Blockage
     * Formato esperado: DDdHHhMMm-DDdHHhMMm:X1,Y1,X2,Y2,...,Xn,Yn
     * Ejemplo: 01d00h31m-01d21h35m:15,10,30,10,30,18
     *
     * @param line     Línea de texto en formato de archivo de bloqueos
     * @param baseDate Fecha base para calcular fechas relativas (normalmente el
     *                 primer día del mes)
     * @return Instancia de Blockage creada a partir de la línea
     * @throws IllegalArgumentException si el formato de la línea es incorrecto
     */
    public static Blockage fromFileFormat(String line, LocalDateTime baseDate) {
        try {
            // Dividir la línea en la parte de tiempo y la parte de coordenadas
            String[] parts = line.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Formato de línea incorrecto: " + line);
            }

            // Parsear la parte de tiempo
            String[] timeParts = parts[0].split("-");
            if (timeParts.length != 2) {
                throw new IllegalArgumentException("Formato de tiempo incorrecto: " + parts[0]);
            }

            // Parsear tiempo inicio y fin
            LocalDateTime startTime = parseRelativeTime(timeParts[0], baseDate);
            LocalDateTime endTime = parseRelativeTime(timeParts[1], baseDate);

            // Parsear las coordenadas
            List<Position> positions = parsePositions(parts[1]);

            // Crear y retornar la instancia de Blockage
            return new Blockage(startTime, endTime, positions);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al parsear la línea: " + line, e);
        }
    }

    /**
     * Parsea un tiempo relativo en formato DDdHHhMMm
     * 
     * @param timeStr  Tiempo en formato DDdHHhMMm
     * @param baseDate Fecha base para calcular la fecha resultante
     * @return LocalDateTime resultante
     */
    private static LocalDateTime parseRelativeTime(String timeStr, LocalDateTime baseDate) {
        // Extraer días, horas y minutos usando expresiones regulares
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)d(\\d+)h(\\d+)m");
        java.util.regex.Matcher matcher = pattern.matcher(timeStr);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Formato de tiempo relativo incorrecto: " + timeStr);
        }

        int days = Integer.parseInt(matcher.group(1));
        int hours = Integer.parseInt(matcher.group(2));
        int minutes = Integer.parseInt(matcher.group(3));

        // Calcular la fecha resultante
        return baseDate
                .withDayOfMonth(days) // Establecer el día del mes
                .withHour(hours)
                .withMinute(minutes)
                .withSecond(0)
                .withNano(0);
    }

    /**
     * Parsea una cadena de coordenadas en formato X1,Y1,X2,Y2,...,Xn,Yn
     * 
     * @param coordsStr Cadena con las coordenadas
     * @return Lista de posiciones
     */
    private static List<Position> parsePositions(String coordsStr) {
        String[] coords = coordsStr.split(",");
        if (coords.length < 4 || coords.length % 2 != 0) {
            throw new IllegalArgumentException("Formato de coordenadas incorrecto: " + coordsStr);
        }

        List<Position> positions = new ArrayList<>(coords.length / 2);
        for (int i = 0; i < coords.length; i += 2) {
            int x = Integer.parseInt(coords[i]);
            int y = Integer.parseInt(coords[i + 1]);
            positions.add(new Position(x, y));
        }

        return positions;
    }

    /**
     * Sets the ID of the blockage (primarily for testing purposes)
     * 
     * @param id the ID to set
     */
    public void setId(Long id) {
        this.id = id;
    }
}
