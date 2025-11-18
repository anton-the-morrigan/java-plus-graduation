package ru.practicum.stats;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    void saveHit(EndpointHitDto hitDto);

    List<ViewStatsDto> getStats(LocalDateTime startTime, LocalDateTime endTime, List<String> uris, boolean unique);
}
