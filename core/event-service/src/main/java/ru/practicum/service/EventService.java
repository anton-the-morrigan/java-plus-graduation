package ru.practicum.service;

import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.params.EventAdminSearchParam;
import ru.practicum.params.EventUserSearchParam;
import ru.practicum.params.PublicEventSearchParam;

import java.util.List;
import java.util.Map;


public interface EventService {

    List<EventFullDto> getEventsByParams(EventAdminSearchParam param);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest);

    EventFullDto getEventById(Long id);

    List<EventShortDto> searchEvents(PublicEventSearchParam param);

    List<EventShortDto> getUsersEvents(EventUserSearchParam param);

    EventFullDto saveEvent(NewEventDto dto, Long userId);

    EventFullDto getEventByIdAndUserId(Long userId, Long eventId);

    EventFullDto updateEventByUser(Long userId, Long eventId, UpdateEventUserRequest updateRequest);

    List<ParticipationRequestDto> getRequestForEventByUserId(Long eventId, Long userId);

    EventRequestStatusUpdateResult updateRequests(Long eventId, Long userId, EventRequestStatusUpdateRequest updateRequest);

    List<EventShortDto> getEventsFeedCogList(List<Long> followedUsersIds, PublicEventSearchParam param);

    Map<Long, EventFullDto> getEventsFeedCogMap(List<Long> followedUsersIds, PublicEventSearchParam param);
}
