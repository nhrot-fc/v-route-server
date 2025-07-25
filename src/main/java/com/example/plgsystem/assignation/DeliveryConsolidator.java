package com.example.plgsystem.assignation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DeliveryConsolidator {

    /**
     * 📦📦➡️⚛️⚛️ **SEPARAR CONSECUTIVO Y ATÓMICO (SPLIT):**
     * Agrupa partes CONSECUTIVAS de la misma orden, suma su GLP y luego divide
     * esa suma en chunks de tamaño atómico.
     * Ejemplo: [7(O1), 3(O1)] con atomicSize=5 -> [5(O1), 5(O1)]
     *
     * @param parts           La lista de partes a procesar, en su orden original.
     * @param atomicChunkSize El tamaño indivisible de una entrega.
     * @return Una lista de partes en su forma atómica, respetando la agrupación
     *         consecutiva.
     */
    public static List<DeliveryPart> splitConsecutiveAndAtomic(List<DeliveryPart> parts, int atomicChunkSize) {
        if (parts == null || parts.isEmpty() || atomicChunkSize <= 0) {
            return new ArrayList<>();
        }

        List<DeliveryPart> atomicParts = new ArrayList<>();
        for (DeliveryPart part: parts) {
            int remainingGlp = part.getGlpDeliverM3();
            String orderId = part.getOrderId();
            LocalDateTime deadline = part.getDeadlineTime();

            while (remainingGlp > 0) {
                int chunkSize = Math.min(atomicChunkSize, remainingGlp);
                atomicParts.add(new DeliveryPart(orderId, chunkSize, deadline));
                remainingGlp -= chunkSize;
            }
        }

        List<DeliveryPart> consolidatedParts = mergeLikeRLE(atomicParts, atomicChunkSize);

        return consolidatedParts;
    }

    /**
     * ⚛️⚛️➡️🚛 **REUNIR CON LÓGICA RLE Y CAPACIDAD (MERGE):**
     * Fusiona una lista de partes (idealmente atómicas) de forma secuencial,
     * llenando "chunks" hasta la capacidad máxima sin alterar el orden.
     * Ejemplo: [7,7,7,7,7] con capacidad=15 -> [15, 15, 5]
     *
     * @param parts    La lista de partes a fusionar, en orden.
     * @param capacity La capacidad máxima de cada chunk fusionado.
     * @return Una lista con las partes reagrupadas eficientemente.
     */
    public static List<DeliveryPart> mergeLikeRLE(List<DeliveryPart> parts, int capacity) {
        if (parts == null || parts.isEmpty() || capacity <= 0) {
            return new ArrayList<>();
        }

        List<DeliveryPart> mergedParts = new ArrayList<>();
        
        int currentOrderGlp = 0;
        String currentOrderId = null;

        for (DeliveryPart part : parts) {
            if (currentOrderId == null || !currentOrderId.equals(part.getOrderId())) {
                int remainingGlp = currentOrderGlp;
                while (remainingGlp > 0) {
                    int chunk = Math.min(capacity, remainingGlp);
                    mergedParts.add(new DeliveryPart(currentOrderId, chunk, part.getDeadlineTime()));
                    remainingGlp -= chunk;
                }
                currentOrderId = part.getOrderId();
                currentOrderGlp = 0; // Reset for new order
            }
            currentOrderGlp += part.getGlpDeliverM3();
        }

        // Handle any remaining GLP for the last order
        if (currentOrderGlp > 0) {
            int remainingGlp = currentOrderGlp;
            while (remainingGlp > 0) {
                int chunk = Math.min(capacity, remainingGlp);
                mergedParts.add(new DeliveryPart(currentOrderId, chunk, parts.get(parts.size() - 1).getDeadlineTime()));
                remainingGlp -= chunk;
            }
            if (remainingGlp > 0) {
                mergedParts.add(new DeliveryPart(currentOrderId, remainingGlp, parts.get(parts.size() - 1).getDeadlineTime()));
            }
        }

        return mergedParts;
    }
}