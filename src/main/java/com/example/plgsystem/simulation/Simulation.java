package com.example.plgsystem.simulation;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Delegate;
import com.example.plgsystem.enums.SimulationStatus;
import com.example.plgsystem.enums.SimulationType;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Representa una instancia de simulación gestionada como un bean prototipo de
 * Spring.
 * <p>
 * Cada instancia es única, con su propio estado y ciclo de vida, ideal para
 * manejar múltiples simulaciones en memoria.
 */
@Getter
@Component
@Scope("prototype")
public class Simulation {

    private final UUID id;
    private SimulationStatus status;
    private SimulationType type;
    @Delegate
    private final SimulationState state;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Simulation(SimulationState state) {
        this(state, SimulationType.CUSTOM);
    }

    public Simulation(SimulationState state, SimulationType type) {
        this.id = UUID.randomUUID();
        this.state = state;
        this.status = SimulationStatus.PAUSED;
        this.startTime = LocalDateTime.now();
        this.type = type;
    }

    public void start() {
        this.status = SimulationStatus.RUNNING;
    }

    public void pause() {
        if (!type.isDailyOperation()) { // Daily operations can't be paused
            this.status = SimulationStatus.PAUSED;
        }
    }

    public void finish() {
        if (!type.isDailyOperation()) { // Daily operations can't be finished
            this.status = SimulationStatus.FINISHED;
            this.endTime = LocalDateTime.now();
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