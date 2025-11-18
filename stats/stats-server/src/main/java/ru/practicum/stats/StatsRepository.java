package ru.practicum.stats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<StatsEntry, Long> {

    @Query("""
            select new ru.practicum.dto.ViewStatsDto(se.app, se.uri, count(se.ip))
            from StatsEntry as se
            where (se.timestamp >= ?1 and se.timestamp < ?2)
            group by se.app, se.uri
            order by count(se.ip) desc""")
    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end);

    @Query("""
            select new ru.practicum.dto.ViewStatsDto(se.app, se.uri, count(distinct se.ip))
            from StatsEntry as se
            where (se.timestamp >= ?1 and se.timestamp < ?2)
            group by se.app, se.uri
            order by count(distinct se.ip) desc""")
    List<ViewStatsDto> getStatsUniqueIp(LocalDateTime start, LocalDateTime end);

    @Query("""
            select new ru.practicum.dto.ViewStatsDto(se.app, se.uri, count(se.ip))
            from StatsEntry as se
            where (se.timestamp >= ?1 and se.timestamp < ?2 and se.uri in ?3)
            group by se.app, se.uri
            order by count(se.ip) desc""")
    List<ViewStatsDto> getStatsByUris(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("""
            select new ru.practicum.dto.ViewStatsDto(se.app, se.uri, count(distinct se.ip))
            from StatsEntry as se
            where (se.timestamp >= ?1 and se.timestamp < ?2 and se.uri in ?3)
            group by se.app, se.uri
            order by count(DISTINCT se.ip) desc""")
    List<ViewStatsDto> getStatsByUrisUniqueIp(LocalDateTime start, LocalDateTime end, List<String> uris);
}
