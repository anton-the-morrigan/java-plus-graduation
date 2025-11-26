package ru.practicum.client;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

@FeignClient(name = "request-service")
public interface RequestClient {

    @GetMapping("/event/{eventId}")
    List<ParticipationRequestDto> getRequestForEventByUserId(Long eventId, Long userId) throws FeignException;

    @PatchMapping
    EventRequestStatusUpdateResult updateRequests(Long eventId, Long userId, EventRequestStatusUpdateRequest updateRequest) throws FeignException;

    @GetMapping("/confirmed")
    Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) throws FeignException;
}
