package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.params.SortSearchParam;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final EventService eventService;
    private final String dateTimePattern = "yyyy-MM-dd HH:mm:ss";

    @GetMapping
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = dateTimePattern) LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = dateTimePattern) LocalDateTime rangeEnd,
            @RequestParam(required = false) SortSearchParam sort,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size,
            HttpServletRequest request) {

        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeEnd can't before rangeStart");
        }
        if (rangeEnd == null && rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateTimePattern));

        PublicEventSearchParam param = PublicEventSearchParam.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .onlyAvailable(onlyAvailable)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .sort(sort)
                .from(from)
                .size(size)
                .build();

        List<EventShortDto> events = eventService.searchEvents(param);

        return events;
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable Long id,
                                     @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId,
                                     HttpServletRequest request) {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(dateTimePattern));

        EventFullDto event = eventService.getEventById(id, userId);
        return event;
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") Long userId) {
        return eventService.getRecommendations(userId);
    }

    @PutMapping("/events/{eventId}/like")
    public void likeEvent(@PathVariable Long eventId,
                          @RequestHeader(value = "X-EWM-USER-ID", required = false) Long userId) {
        eventService.likeEvent(eventId, userId);
    }
}
