package ru.practicum.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.SubscriptionService;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.params.SortSearchParam;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class PrivateSubscriptionController {
    private final SubscriptionService subscriptionService;
    private final String dateTimePattern = "yyyy-MM-dd HH:mm:ss";
    private final String subscriptions = "/{userId}/subscriptions";

    @PutMapping(subscriptions)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createSubscribe(@PathVariable @Positive long userId,
                                @RequestParam @Positive long followedToUserId) {
        subscriptionService.createSubscribe(userId, followedToUserId);
    }

    @DeleteMapping(subscriptions)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubscribe(@PathVariable @Positive long userId,
                                @RequestParam @Positive long followedToUserId) {
        subscriptionService.deleteSubscribe(userId, followedToUserId);
    }

    @GetMapping(subscriptions)
    public List<UserShortDto> getSubscriptions(@PathVariable @Positive long userId,
                                               @RequestParam(defaultValue = "0") Integer from,
                                               @RequestParam(defaultValue = "10") Integer size) {
        return subscriptionService.getSubscribes(userId, from, size);
    }

    @GetMapping("/{userId}/events/feed")
    public List<EventShortDto> getEventsFeed(@PathVariable @Positive long userId,
                                             @RequestParam(required = false) String text,
                                             @RequestParam(required = false) List<Long> categories,
                                             @RequestParam(required = false) Boolean paid,
                                             @RequestParam(required = false) @DateTimeFormat(pattern = dateTimePattern) LocalDateTime rangeStart,
                                             @RequestParam(required = false) @DateTimeFormat(pattern = dateTimePattern) LocalDateTime rangeEnd,
                                             @RequestParam(required = false) SortSearchParam sort,
                                             @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                             @RequestParam(defaultValue = "0") Integer from,
                                             @RequestParam(defaultValue = "10") Integer size) {

        if (sort == null) {
            sort = SortSearchParam.EVENT_DATE;
        }

        if (rangeStart == null) {
            rangeStart = LocalDateTime.now();
        }

        if (rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            throw new BadRequestException("rangeEnd can't before rangeStart");
        }

        PublicEventSearchParam param = PublicEventSearchParam.builder()
                .text(text)
                .categories(categories)
                .paid(paid)
                .rangeStart(rangeStart)
                .rangeEnd(rangeEnd)
                .onlyAvailable(onlyAvailable)
                .sort(sort)
                .from(from)
                .size(size)
                .build();

        return subscriptionService.getEventsFeed(userId, param);
    }
}
