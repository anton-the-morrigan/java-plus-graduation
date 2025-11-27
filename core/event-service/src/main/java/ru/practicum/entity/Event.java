package ru.practicum.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.practicum.dto.event.EventState;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "title")
    String title;

    @Column(name = "annotation", length = 2000)
    String annotation;

    @Column(name = "description", length = 10000)
    String description;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    EventState state;

    @Column(name = "event_date")
    LocalDateTime eventDate;

    @Column(name = "created")
    @CreationTimestamp
    LocalDateTime createdOn;

    @Column(name = "published")
    LocalDateTime publishedOn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(name = "initiator_id", nullable = false)
    Long initiator;

    @Column(name = "paid")
    Boolean paid;

    @Column(name = "request_moderation")
    Boolean requestModeration;

    @Column(name = "participant_limit")
    Integer participantLimit;

    @Embedded
    Location location;
}


