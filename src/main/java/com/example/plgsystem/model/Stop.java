package com.example.plgsystem.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Stop {

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "x", column = @Column(name = "position_x")),
        @AttributeOverride(name = "y", column = @Column(name = "position_y"))
    })
    protected Position position;

    public Position getPosition() {
        return position;
    }

    public abstract Stop clone();

    @Override
    public String toString() {
        return "🛑" + position.toString();
    }
}
