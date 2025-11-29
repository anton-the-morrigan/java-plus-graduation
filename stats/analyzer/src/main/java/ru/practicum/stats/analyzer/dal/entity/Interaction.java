package ru.practicum.stats.analyzer.dal.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "interactions")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "event_id", nullable = false)
    Long eventId;

    @Column(name = "rating", nullable = false)
    Float rating;

    @Column(name = "ts", nullable = false)
    Instant timestamp;
}
