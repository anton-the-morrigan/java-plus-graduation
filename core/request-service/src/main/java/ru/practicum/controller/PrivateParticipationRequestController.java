package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateParticipationRequestController {

    private final ParticipationRequestService participationRequestService;

    @GetMapping("/requests")
    public List<ParticipationRequestDto> getRequests(@PathVariable Long userId) {
        return participationRequestService.getRequestsByUser(userId);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto createRequest(@PathVariable Long userId,
                                                 @RequestParam Long eventId) {
        log.info("Create request for user {} with event {}", userId, eventId);
        return participationRequestService.createRequest(userId, eventId);
    }

    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                                 @PathVariable Long requestId) {
        return participationRequestService.cancelRequest(userId, requestId);
    }

    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getUsersRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {

        List<ParticipationRequestDto> requestForEventByUserId = participationRequestService.getRequestForEventByUserId(eventId, userId);
        return requestForEventByUserId;
    }

    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateUsersRequests(@PathVariable @Positive Long userId,
                                                              @PathVariable @Positive Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest updateRequest) {

        return participationRequestService.updateRequests(eventId, userId, updateRequest);
    }
}
