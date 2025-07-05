package com.example.plgsystem.assignation;

import com.example.plgsystem.model.Environment;
import com.example.plgsystem.assignation.Solution;

public interface Assignator {
    Solution solve(Environment env);
}
