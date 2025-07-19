package com.example.plgsystem.pathfinding;

import java.time.LocalDateTime;
import java.util.*;

import com.example.plgsystem.model.Constants;
import com.example.plgsystem.model.Position;
import com.example.plgsystem.simulation.SimulationState;

public class PathFinder {
    private static final int[][] DIRECTIONS = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };

    public static List<Position> findPath(SimulationState state, Position start, Position end,
            LocalDateTime departureTime) {
        // Handle edge cases
        if (start == null || end == null || departureTime == null || state == null) {
            return Collections.emptyList();
        }

        // If start and end are the same, return the start position
        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        // If start position is blocked, no path is possible
        if (state.isPositionBlockedAt(start, departureTime)) {
            return Collections.emptyList();
        }

        // A* algorithm data structures
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        Map<Position, Node> allNodes = new HashMap<>();
        Set<Position> closedSet = new HashSet<>();

        // Initialize with start node
        Node startNode = new Node(start, null, 0, manhattanDistance(start, end), departureTime);
        openSet.add(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();

            // Path found
            if (current.position.equals(end)) {
                return reconstructPath(current);
            }

            closedSet.add(current.position);

            // Explore all four directions
            for (int[] direction : DIRECTIONS) {
                double neighborX = Math.round(current.position.getX() + direction[0]);
                double neighborY = Math.round(current.position.getY() + direction[1]);

                // Check if position is within city boundaries
                if (isOutOfBounds(neighborX, neighborY)) {
                    continue;
                }

                Position neighborPos = new Position(neighborX, neighborY);

                // Skip already processed positions
                if (closedSet.contains(neighborPos)) {
                    continue;
                }

                // Calculate arrival time at this neighbor
                LocalDateTime arrivalTime = calculateArrivalTime(current.arrivalTime);

                // Skip blocked positions (except the destination)
                if (state.isPositionBlockedAt(neighborPos, arrivalTime) && !neighborPos.equals(end)) {
                    continue;
                }

                // Calculate new cost to this neighbor
                double newG = current.g + 1;

                // Get existing node or create new one with better path
                Node neighborNode = allNodes.get(neighborPos);
                if (neighborNode == null || newG < neighborNode.g) {
                    double h = manhattanDistance(neighborPos, end);
                    Node newNode = new Node(neighborPos, current, newG, h, arrivalTime);

                    // Update open set with new or improved node
                    if (neighborNode != null) {
                        openSet.remove(neighborNode);
                    }

                    openSet.add(newNode);
                    allNodes.put(neighborPos, newNode);
                }
            }
        }

        // No path found
        return Collections.emptyList();
    }

    /**
     * Checks if position coordinates are outside city boundaries
     */
    private static boolean isOutOfBounds(double x, double y) {
        return x < 0 || x > Constants.CITY_X || y < 0 || y > Constants.CITY_Y;
    }

    /**
     * Calculates arrival time at the next position based on vehicle speed
     */
    private static LocalDateTime calculateArrivalTime(LocalDateTime departureTime) {
        long secondsToTravel = (long) (Constants.NODE_DISTANCE / Constants.VEHICLE_AVG_SPEED * 3600);
        return departureTime.plusSeconds(secondsToTravel);
    }

    /**
     * Manhattan distance heuristic for A* algorithm
     */
    private static double manhattanDistance(Position a, Position b) {
        return a.distanceTo(b);
    }

    /**
     * Reconstructs the path from the destination node back to start
     */
    private static List<Position> reconstructPath(Node destination) {
        List<Position> path = new LinkedList<>();
        Node current = destination;

        while (current != null) {
            path.addFirst(current.position);
            current = current.parent;
        }

        return path;
    }

    /**
     * A* node with position, path cost, and temporal information
     */
    private static class Node implements Comparable<Node> {
        final Position position;
        final Node parent;
        final double g; // Cost from start to this node
        final double f; // Total estimated cost (g + h)
        final LocalDateTime arrivalTime;

        Node(Position position, Node parent, double g, double h, LocalDateTime arrivalTime) {
            this.position = position;
            this.parent = parent;
            this.g = g;
            this.f = g + h;
            this.arrivalTime = arrivalTime;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(this.f, other.f);
        }
    }
}
