package ru.practicum.client;

import feign.FeignException;
import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.params.PublicEventSearchParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "event-service")
public interface EventClient {

    @GetMapping("/{eventId}")
    EventFullDto getEventByUserIdAndEventId(@PathVariable @Positive Long userId,
                                            @PathVariable @Positive Long eventId) throws FeignException;

    @PatchMapping("/feed/1")
    List<EventShortDto> getEventsFeedCogList(@RequestParam List<Long> followedUsersIds, @RequestParam PublicEventSearchParam param) throws FeignException;

    @PatchMapping("/feed/2")
    Map<Long, EventFullDto> getEventsFeedCogMap(@RequestParam List<Long> followedUsersIds, @RequestParam PublicEventSearchParam param) throws FeignException;
}
