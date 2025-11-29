package ru.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "similarities")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long eventA;

    Long eventB;

    Double score;

    Instant timestamp;
}
