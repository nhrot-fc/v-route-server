package com.example.plgsystem.pathfinding;

import java.time.LocalDateTime;
import java.util.*;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.simulation.SimulationState;

public class PathFinder {
    public static List<Position> findPath(SimulationState entorno, Position inicio, Position fin,
            LocalDateTime horaSalida) {
        if (inicio == null || fin == null || horaSalida == null || entorno == null) {
            return Collections.emptyList();
        }
        if (inicio.equals(fin)) {
            return Collections.singletonList(inicio);
        }
        if (entorno.isPositionBlockedAt(inicio, horaSalida)) {
            return Collections.emptyList();
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Position, Node> positionToNodeMap = new HashMap<>();
        Set<Position> closedSet = new HashSet<>();

        Node startNode = new Node(inicio, null, 0, heuristic(inicio, fin), horaSalida);
        openSet.add(startNode);
        positionToNodeMap.put(inicio, startNode);

        int[][] direcciones = { { 0, 1 }, { 1, 0 }, { 0, -1 }, { -1, 0 } };

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            if (current.position.equals(fin)) {
                return construirResultado(current);
            }

            closedSet.add(current.position);

            for (int[] dir : direcciones) {
                int newX = current.position.getX() + dir[0];
                int newY = current.position.getY() + dir[1];

                if (newX < 0 || newX >= Constants.CITY_X ||
                        newY < 0 || newY >= Constants.CITY_Y) {
                    continue;
                }

                Position vecino = new Position(newX, newY);

                if (closedSet.contains(vecino)) {
                    continue;
                }

                LocalDateTime tiempoLlegada = calcularTiempoLlegada(current.horaDeLlegada);

                if (entorno.isPositionBlockedAt(vecino, tiempoLlegada) && !vecino.equals(fin)) {
                    continue;
                }

                double newG = current.g + 1;

                Node vecinoNode = positionToNodeMap.get(vecino);
                if (vecinoNode == null || newG < vecinoNode.g) {
                    double h = heuristic(vecino, fin);
                    Node newNode = new Node(vecino, current, newG, h, tiempoLlegada);

                    if (vecinoNode != null) {
                        openSet.remove(vecinoNode);
                    }

                    openSet.add(newNode);
                    positionToNodeMap.put(vecino, newNode);
                }
            }
        }

        return Collections.emptyList();
    }

    private static LocalDateTime calcularTiempoLlegada(LocalDateTime horaSalida) {
        long segundosViaje = (long) (Constants.NODE_DISTANCE / Constants.VEHICLE_AVG_SPEED * 3600);
        return horaSalida.plusSeconds(segundosViaje);
    }

    private static double heuristic(Position a, Position b) {
        return a.distanceTo(b);
    }

    private static List<Position> construirResultado(Node destinoNode) {

        List<Position> camino = new LinkedList<>();

        Node current = destinoNode;

        while (current != null) {
            camino.addFirst(current.position);
            current = current.parent;
        }

        return camino;
    }

    // Clase interna para los nodos de A*, ahora con tiempo
    private static class Node implements Comparable<Node> {
        final Position position;
        final Node parent;
        final double g;
        final double f;
        final LocalDateTime horaDeLlegada;

        Node(Position position, Node parent, double g, double h, LocalDateTime horaDeLlegada) {
            this.position = position;
            this.parent = parent;
            this.g = g;
            this.f = g + h;
            this.horaDeLlegada = horaDeLlegada;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }
    }
}
