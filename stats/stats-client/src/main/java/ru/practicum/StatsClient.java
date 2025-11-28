package ru.practicum;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {
    private static final String STATS_SERVER_ID = "stats-server";
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;

    @CircuitBreaker(name = "statsClient", fallbackMethod = "postHitFallback")
    public void postHit(EndpointHitDto dto) {
        ServiceInstance statsServer = getStatsServerInstance();
        restTemplate.postForEntity(statsServer.getUri() + "/hit", dto, Void.class);
    }

    @CircuitBreaker(name = "statsClient", fallbackMethod = "getStatsFallback")
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) throws RestClientException {
        ServiceInstance statsServer = getStatsServerInstance();

        StringBuilder uri = new StringBuilder()
                .append(statsServer.getUri())
                .append("/stats")
                .append("?start=").append(start)
                .append("&end=").append(end)
                .append("&unique=").append(unique);

        if (!uris.isEmpty()) {
            for (String uriStr : uris) {
                uri.append("&uri=").append(uriStr);
            }
        }

        ResponseEntity<ViewStatsDto[]> response = restTemplate.getForEntity(uri.toString(), ViewStatsDto[].class);

        ViewStatsDto[] body = response.getBody();
        return (body == null) ? new ArrayList<>() : Arrays.asList(body);
    }

    private void postHitFallback(EndpointHitDto dto, Exception ex) {
        log.warn("Stats server unavailable, hit not recorded: {}", ex.getMessage());
    }

    private List<ViewStatsDto> getStatsFallback(String start, String end, List<String> uris, boolean unique, Exception ex) {
        log.warn("Stats server unavailable, returned empty list: {}", ex.getMessage());
        return Collections.emptyList();
    }

    private ServiceInstance getStatsServerInstance() {
        try {
            return discoveryClient
                    .getInstances(STATS_SERVER_ID)
                    .getFirst();
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + STATS_SERVER_ID,
                    exception
            );
        }
    }
}