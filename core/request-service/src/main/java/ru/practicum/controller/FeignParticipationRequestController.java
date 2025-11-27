package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.RequestClient;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/feign")
public class FeignParticipationRequestController implements RequestClient  {

    private final ParticipationRequestService participationRequestService;

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequestForEventByUserId(Long eventId, Long userId) {
        return participationRequestService.getRequestForEventByUserId(eventId, userId);
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequests(Long eventId, Long userId, EventRequestStatusUpdateRequest updateRequest) {
        return participationRequestService.updateRequests(eventId, userId, updateRequest);
    }

    @GetMapping("/confirmed")
    public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
        return participationRequestService.getConfirmedRequestsCount(eventIds);
    }

}
