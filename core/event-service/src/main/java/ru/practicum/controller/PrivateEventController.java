package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.EventClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.event.UpdateEventUserRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.params.EventUserSearchParam;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.service.EventService;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}/events")
public class PrivateEventController implements EventClient {

    private final EventService eventService;
    private final String id = "/{eventId}";
    private final String requests = "/{eventId}/requests";


    @GetMapping
    public List<EventShortDto> getUsersEvents(@PathVariable @Positive Long userId,
                                              @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                              @RequestParam(defaultValue = "10") @Positive Integer size) {

        EventUserSearchParam params = EventUserSearchParam.builder()
                .userId(userId)
                .from(from)
                .size(size)
                .build();

        return eventService.getUsersEvents(params);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public EventFullDto createEvent(@PathVariable @Positive Long userId,
                                    @RequestBody @Valid NewEventDto dto) {

        return eventService.saveEvent(dto, userId);
    }

    @Override
    public EventFullDto getEventByUserIdAndEventId(Long userId,
                                                   Long eventId) {

        return eventService.getEventByIdAndUserId(eventId, userId);
    }

    @PatchMapping(id)
    public EventFullDto updateEventByUser(@PathVariable @Positive Long userId,
                                          @PathVariable @Positive Long eventId,
                                          @RequestBody @Valid UpdateEventUserRequest event) {

        return eventService.updateEventByUser(eventId, userId, event);
    }

    @GetMapping(requests)
    public List<ParticipationRequestDto> getUsersRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {

        return eventService.getRequestForEventByUserId(eventId, userId);
    }

    @PatchMapping(requests)
    public EventRequestStatusUpdateResult updateUsersRequests(@PathVariable @Positive Long userId,
                                                              @PathVariable @Positive Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest updateRequest) {

        return eventService.updateRequests(eventId, userId, updateRequest);
    }

    @Override
    public List<EventShortDto> getEventsFeedCogList(List<Long> followedUsersIds, PublicEventSearchParam param) {
        return eventService.getEventsFeedCogList(followedUsersIds, param);
    }

    @Override
    public Map<Long, EventFullDto> getEventsFeedCogMap(List<Long> followedUsersIds, PublicEventSearchParam param) {
        return eventService.getEventsFeedCogMap(followedUsersIds, param);
    }
}


