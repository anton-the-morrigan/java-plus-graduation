package ru.practicum.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.EventClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.entity.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.service.ParticipationRequestService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper participationRequestMapper;
    private final EventClient eventClient;

    @Override
    public List<ParticipationRequestDto> getRequestForEventByUserId(Long eventId, Long userId) {
        log.info("Get request for event by id: {}", eventId);
        EventFullDto event = eventClient.getEventForParticipationService(eventId);
        if (!Objects.equals(event.getInitiator(), userId)) {
            throw new ConflictException("Can't get request for event id=" + eventId + "by user id=" + userId);
        }
        List<ParticipationRequest> requests = requestRepository.findAllByEventId(eventId);
        return requests.stream()
                .map(participationRequestMapper::toDto)
                .toList();
    }

    @Transactional
    @Override
    public EventRequestStatusUpdateResult updateRequests(Long eventId,
                                                         Long userId,
                                                         EventRequestStatusUpdateRequest updateRequest) {
        log.info("Update request: {}", updateRequest);
        List<ParticipationRequest> requestList = requestRepository.findAllById(updateRequest.getRequestIds());
        EventFullDto event = eventClient.getEventForParticipationService(eventId);
        if (!Objects.equals(event.getInitiator(), userId)) {
            throw new ConflictException("Can't update event id=" + eventId + " requests by user id=" + userId);
        }
        updateRequests(requestList, updateRequest.getStatus(), event);

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        requestList.forEach(request -> {
            switch (request.getStatus()) {
                case RequestStatus.REJECTED ->
                        result.getRejectedRequests().add(participationRequestMapper.toDto(request));
                case RequestStatus.CONFIRMED ->
                        result.getConfirmedRequests().add(participationRequestMapper.toDto(request));
            }
        });
        return result;



    }

    @Override
    public List<ParticipationRequestDto> getRequestsByUser(Long userId) {
        return requestRepository.findAllByRequesterId(userId)
                .stream()
                .map(participationRequestMapper::toDto)
                .toList();
    }

    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        EventFullDto event = eventClient.getEventForParticipationService(eventId);

        if (event.getInitiator().equals(userId)) {
            throw new ConflictException("Initiator cannot request participation in their own event");
        }

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Event must be published to request participation");
        }

        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED) >= event.getParticipantLimit()) {
            throw new ConflictException("Event participant limit reached");
        }

        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userId);
        request.setEventId(eventId);

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("This request already exists");
        }

        if (event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
        } else {
            request.setStatus(event.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED);
        }
        request.setCreated(LocalDateTime.now());

        return participationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request not found"));

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("User is not the requester");
        }

        request.setStatus(RequestStatus.CANCELED);
        return participationRequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public Map<Long, Long> getConfirmedRequestsCount(List<Long> eventIds) {
        return requestRepository.countRequestsByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED);
    }

    private void updateRequests(List<ParticipationRequest> requests, RequestStatus status, EventFullDto event) {
        boolean hasNotPendingRequests = requests.stream().map(ParticipationRequest::getStatus).anyMatch(el -> el != RequestStatus.PENDING);
        if (hasNotPendingRequests)
            throw new ConflictException("Can't change status when request status is not PENDING");

        if (status == RequestStatus.REJECTED) {
            for (ParticipationRequest request : requests) {
                request.setStatus(RequestStatus.REJECTED);
            }
            return;
        }
        Boolean requestModeration = event.getRequestModeration();
        Integer participantLimit = event.getParticipantLimit();

        if (!requestModeration && participantLimit == null) {
            requests.forEach(request -> request.setStatus(status));
            return;
        }

        long confirmed = requestRepository
                .countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        for (ParticipationRequest request : requests) {
            if (confirmed >= participantLimit) {
                throw new ConflictException("Requests out of limit");
            } else {
                request.setStatus(status);
                confirmed++;
            }
        }
    }

}
