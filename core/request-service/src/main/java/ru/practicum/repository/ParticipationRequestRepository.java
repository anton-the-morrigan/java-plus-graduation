package ru.practicum.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.entity.ParticipationRequest;
import ru.practicum.dto.request.RequestStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    @Query("""
            select r.eventId, count(r.id)
            from ParticipationRequest r
            where r.eventId in ?1 and r.status = ?2
            group by r.eventId""")
    List<Object[]> countRequestsByStatus(List<Long> ids, RequestStatus status);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @EntityGraph(attributePaths = {"requester", "event"})
    List<ParticipationRequest> findAllByRequesterId(Long userId);

    default Map<Long, Long> countRequestsByEventIdsAndStatus(List<Long> ids, RequestStatus status) {
        List<Object[]> result = countRequestsByStatus(ids, status);
        return result.stream()
                .collect(Collectors.toMap(
                        arr -> (Long) arr[0],
                        arr -> (Long) arr[1]
                ));
    }

    @EntityGraph(attributePaths = {"requester", "event"})
    List<ParticipationRequest> findAllByEventId(Long eventId);

    Boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);
}
