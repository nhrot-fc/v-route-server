package com.example.plgsystem.model;

public enum IncidentType {
    TYPE_1, // Immobilization 2h, then resumes route
    TYPE_2, // Immobilization 2h, then to workshop for 1 shift (availability rules apply)
    TYPE_3, // Immobilization 4h, then to workshop for 1 full day (available A+3 T1)
}
