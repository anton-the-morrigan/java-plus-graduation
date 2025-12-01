package ru.practicum.stats.analyzer.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.stats.analyzer.dal.entity.Interaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);

    List<Interaction> findByEventIdIn(Collection<Long> eventIds);
    
    List<Interaction> findByEventIdInAndUserId(Collection<Long> eventIds, Long userId);

    @Query("""
            SELECT i.eventId FROM Interaction AS i
            WHERE i.userId = ?1
            ORDER BY i.timestamp DESC""")
    Set<Long> findEventIdsForUserInteractions(Long userId);
}
