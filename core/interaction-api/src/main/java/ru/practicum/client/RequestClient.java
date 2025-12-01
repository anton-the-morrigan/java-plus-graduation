package ru.practicum.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/confirmed")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds) throws FeignException;

    @GetMapping("/event/{eventId}")
    List<ParticipationRequestDto> getRequestForEvent(@PathVariable Long eventId);
}
