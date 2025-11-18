package ru.practicum.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {
    private final StatsRepository repository;

    @Override
    @Transactional
    public void saveHit(EndpointHitDto hitDto) {
        log.debug("Запрос на сохранение в статистику: app = {}, uri = {}, ip = {}, timestamp = {}",
                hitDto.getApp(), hitDto.getUri(), hitDto.getIp(), hitDto.getTimestamp());

        StatsEntry entry = StatsEntry.fromDto(hitDto);
        repository.save(entry);

        log.info("Сохранена запись в статистику: id = {}, app = {}, uri = {}, ip = {}, timestamp = {}",
                entry.getId(), entry.getApp(), entry.getUri(), entry.getIp(), entry.getTimestamp());
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime startTime, LocalDateTime endTime, List<String> uris, boolean unique) {
        log.debug("Запрос на получение статистики c {} по {}, фильтр по uri: {}, уникальный ip: {}",
                startTime, endTime, uris, unique);

        if (unique && uris != null) {
            return repository.getStatsByUrisUniqueIp(startTime, endTime, uris);
        } else if (!unique && uris != null) {
            return repository.getStatsByUris(startTime, endTime, uris);
        } else if (unique) {
            return repository.getStatsUniqueIp(startTime, endTime);
        } else return repository.getStats(startTime, endTime);
    }
}
