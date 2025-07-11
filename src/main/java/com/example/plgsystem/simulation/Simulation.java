package com.example.plgsystem.simulation;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import com.example.plgsystem.enums.SimulationStatus;
import com.example.plgsystem.enums.SimulationType;
import com.example.plgsystem.operation.VehiclePlan;
import com.example.plgsystem.orchest.DataLoader;
import com.example.plgsystem.orchest.Orchestrator;

@Getter
public class Simulation {

    private final UUID id;
    private final SimulationType type;
    private final Orchestrator orchestrator;

    // Real world attributes
    private SimulationStatus status;
    private LocalDateTime creationTime; // When the simulation was created in real-world
    private LocalDateTime realStartTime; // When the simulation was actually started running
    private LocalDateTime realEndTime; // When the simulation was finished in real-world

    public Simulation(SimulationState state, SimulationType type, DataLoader dataLoader) {
        this.id = UUID.randomUUID();
        Duration tickDuration = type.isDailyOperation() ? Duration.ofSeconds(1) : Duration.ofMinutes(1);
        int minutesForReplan = type.isDailyOperation() ? 30 : 75;
        this.orchestrator = new Orchestrator(state, tickDuration, minutesForReplan, dataLoader);
        this.status = SimulationStatus.PAUSED;
        this.creationTime = LocalDateTime.now();
        this.type = type;
    }

    public SimulationState getState() {
        return orchestrator.getEnvironment();
    }

    public LocalDateTime getSimulationTime() {
        return orchestrator.getSimulationTime();
    }

    public Map<String, VehiclePlan> getCurrentVehiclePlans() {
        return orchestrator.getEnvironment().getCurrentVehiclePlans();
    }

    public void advanceTick() {
        orchestrator.advanceTick();
    }

    public void start() {
        this.status = SimulationStatus.RUNNING;
        if (this.realStartTime == null) {
            this.realStartTime = LocalDateTime.now();
        }
    }

    public void pause() {
        if (!type.isDailyOperation()) {
            this.status = SimulationStatus.PAUSED;
        }
    }

    public void finish() {
        if (!type.isDailyOperation()) {
            this.status = SimulationStatus.FINISHED;
            this.realEndTime = LocalDateTime.now();
        }
    }

    public void error() {
        this.status = SimulationStatus.ERROR;
    }

    public boolean isDailyOperation() {
        return type.isDailyOperation();
    }

    public boolean isTimeBasedSimulation() {
        return type.isTimeBasedSimulation();
    }

    public boolean isRunning() {
        return status.equals(SimulationStatus.RUNNING);
    }

    public boolean isPaused() {
        return status.equals(SimulationStatus.PAUSED);
    }

    public boolean isFinished() {
        return status.equals(SimulationStatus.FINISHED);
    }

    public boolean isError() {
        return status.equals(SimulationStatus.ERROR);
    }
}