package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.entity.Event;
import ru.practicum.dto.event.EventState;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(attributePaths = {"category"})
    Page<Event> findByInitiator(Long initiator, Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Optional<Event> findByIdAndState(Long id, EventState state);

    @EntityGraph(attributePaths = {"category"})
    Page<Event> findAll(Specification<Event> specification, Pageable pageable);

    List<Event> findByIdIn(Set<Long> eventIds);
}
