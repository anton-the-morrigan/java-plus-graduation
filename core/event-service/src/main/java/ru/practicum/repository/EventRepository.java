package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.entity.Event;
import ru.practicum.dto.event.EventState;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(attributePaths = {"initiator", "category"})
    Page<Event> findByInitiatorId(Long id, Pageable pageable);

    @EntityGraph(attributePaths = {"initiator", "category"})
    Optional<Event> findByIdAndState(Long id, EventState state);

    @EntityGraph(attributePaths = {"initiator", "category"})
    Page<Event> findAll(Specification<Event> specification, Pageable pageable);
}
