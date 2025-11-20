package ru.practicum;

import lombok.RequiredArgsConstructor;
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
import java.util.List;

@Component
@RequiredArgsConstructor
public class StatsClient {
    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;

    private final String STATS_SERVER_ID = "stats-server";

    public void postHit(EndpointHitDto dto) {
        ServiceInstance statsServer = getInstance();
        restTemplate.postForEntity(statsServer.getUri() + "/hit", dto, Void.class);
    }

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) throws RestClientException {

        ServiceInstance serviceInstance = getInstance();

        StringBuilder uri = new StringBuilder()
                .append(serviceInstance.getUri())
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

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(STATS_SERVER_ID)
                    .getFirst();
        } catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Ошибка обнаружения адреса сервиса статистики с id: %s", STATS_SERVER_ID), exception
            );
        }
    }
}