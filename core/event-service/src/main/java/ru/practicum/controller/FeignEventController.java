package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.client.EventClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.service.EventService;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/feign")
public class FeignEventController implements EventClient {

    private final EventService eventService;

    @GetMapping("/users/{userId}/events/{eventId}")
    public EventFullDto getEventByUserIdAndEventId(Long userId,
                                                   Long eventId) {

        return eventService.getEventByIdAndUserId(eventId, userId);
    }

    @PatchMapping("/feed/1")
    public List<EventShortDto> getEventsFeedCogList(List<Long> followedUsersIds, PublicEventSearchParam param) {
        return eventService.getEventsFeedCogList(followedUsersIds, param);
    }

    @PatchMapping("/feed/2")
    public Map<Long, EventFullDto> getEventsFeedCogMap(List<Long> followedUsersIds, PublicEventSearchParam param) {
        return eventService.getEventsFeedCogMap(followedUsersIds, param);
    }
}
