package ru.practicum.stats.analyzer.dal.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "similarities")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Similarity implements Comparable<Similarity> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event1", nullable = false)
    Long event1;

    @Column(name = "event2", nullable = false)
    Long event2;

    @Column(name = "similarity", nullable = false)
    Double similarity;

    @Column(name = "ts", nullable = false)
    Instant timestamp;

    @Override
    public int compareTo(Similarity o) {
        return Double.compare(similarity, o.similarity);
    }
}
