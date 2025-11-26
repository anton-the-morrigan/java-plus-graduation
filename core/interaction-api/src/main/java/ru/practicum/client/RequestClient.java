package ru.practicum.client;

import feign.FeignException;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/events/event/{eventId}")
    List<ParticipationRequestDto> getRequestForEventByUserId(@PathVariable @Positive Long eventId, @PathVariable @Positive Long userId) throws FeignException;

    @PatchMapping("/events/{eventId}/requests")
    EventRequestStatusUpdateResult updateRequests(@PathVariable @Positive Long eventId, @PathVariable @Positive Long userId, @RequestBody EventRequestStatusUpdateRequest updateRequest) throws FeignException;

    @GetMapping("/confirmed")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds) throws FeignException;
}
