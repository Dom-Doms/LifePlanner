package it.univ.lifeplanner.workout.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class WorkoutExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private WorkoutTemplate template;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 80)
    private String muscleGroup;

    private Integer sets;
    private String reps;
    private String suggestedWeight;
    private Integer restSeconds;

    @Lob
    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private int exerciseOrder;
}
