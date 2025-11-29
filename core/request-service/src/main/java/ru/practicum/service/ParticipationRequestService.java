package ru.practicum.service;

import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getRequestForEventByUserId(Long eventId, Long userId);

    EventRequestStatusUpdateResult updateRequests(Long eventId, Long userId, EventRequestStatusUpdateRequest updateRequest);

    List<ParticipationRequestDto> getRequestsByUser(Long userId);

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds);

    List<ParticipationRequestDto> getRequestForEvent(Long eventId);
}
