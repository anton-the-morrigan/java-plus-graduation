package ru.practicum.stats.analyzer.dal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.stats.analyzer.dal.entity.Similarity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
    Optional<Similarity> findByEvent1AndEvent2(Long event1, Long event2);

    @Query("""
            SELECT s FROM Similarity AS s
            WHERE s.event1 = ?1 OR s.event2 = ?1
            ORDER BY s.similarity DESC""")
    List<Similarity> findByEvent(Long eventId);

    @Query("""
            SELECT s FROM Similarity AS s
            WHERE s.event1 IN ?1 OR s.event2 in ?1
            ORDER BY s.similarity DESC""")
    List<Similarity> findByEvent(Collection<Long> eventIds);
}
