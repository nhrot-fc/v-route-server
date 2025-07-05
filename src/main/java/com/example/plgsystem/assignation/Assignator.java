package com.example.plgsystem.assignation;

import com.example.plgsystem.simulation.SimulationState;

public interface Assignator {
    Solution solve(SimulationState env);
}
