package ru.practicum.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/event/{eventId}")
    List<ParticipationRequestDto> getRequestForEventByUserId(@PathVariable Long eventId, @RequestParam Long userId) throws FeignException;

    @PatchMapping
    EventRequestStatusUpdateResult updateRequests(@RequestBody Long eventId, @RequestBody Long userId, @RequestBody EventRequestStatusUpdateRequest updateRequest) throws FeignException;

    @GetMapping("/confirmed")
    Map<Long, Long> getConfirmedRequestsCount(@RequestParam List<Long> eventIds) throws FeignException;
}
