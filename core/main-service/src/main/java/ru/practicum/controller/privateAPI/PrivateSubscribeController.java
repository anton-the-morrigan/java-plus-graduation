package ru.practicum.controller.privateAPI;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.params.SortSearchParam;
import ru.practicum.service.SubscribeService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class PrivateSubscribeController {
    private final SubscribeService subscribeService;
    private final String dateTimePattern = "yyyy-MM-dd HH:mm:ss";
    private final String subscribes = "/{userId}/subscribes";

    @PutMapping(subscribes)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createSubscribe(@PathVariable @Positive long userId,
                                @RequestParam @Positive long followedToUserId) {
        subscribeService.createSubscribe(userId, followedToUserId);
    }

    @DeleteMapping(subscribes)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubscribe(@PathVariable @Positive long userId,
                                @RequestParam @Positive long followedToUserId) {
        subscribeService.deleteSubscribe(userId, followedToUserId);
    }

    @GetMapping(subscribes)
    public List<UserShortDto> getSubscribes(@PathVariable @Positive long userId,
                                            @RequestParam(defaultValue = "0") Integer from,
                                            @RequestParam(defaultValue = "10") Integer size) {
        return subscribeService.getSubscribes(userId, from, size);
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

        return subscribeService.getEventsFeed(userId, param);
    }
}
