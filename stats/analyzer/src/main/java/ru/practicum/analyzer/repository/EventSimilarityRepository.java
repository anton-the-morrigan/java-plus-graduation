package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.entity.EventSimilarity;

import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query("""
        SELECT e
        FROM EventSimilarity e
        WHERE (e.event_a = :event_id OR e.event_b = :event_id)
        AND (e.event_a NOT IN :excluded_ids AND e.event_b NOT IN :excluded_ids)""")
    List<EventSimilarity> findAllByEventIdAndEventIds(@Param("event_id") Long eventId,
                                                      @Param("excluded_ids") List<Long> excludedIds);

    @Query("""
    SELECT e
    FROM EventSimilarity e
    WHERE (e.event_a IN :eventIds OR e.event_b IN :event_ids)
      AND e.event_a NOT IN :excluded_ids
      AND e.event_b NOT IN :excluded_ids""")
    List<EventSimilarity> findAllByEventIdsAndExcludedIds(@Param("event_ids") List<Long> eventIds,
                                                          @Param("excluded_ids") List<Long> excludedIds);

    List<EventSimilarity> findAllByEventA(Long eventAId);
}
