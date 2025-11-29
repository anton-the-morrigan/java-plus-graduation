package ru.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.time.Instant;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "interactions")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    Long userId;

    Long eventId;

    @Enumerated(EnumType.STRING)
    ActionTypeAvro actionType;

    Instant timestamp;
}
