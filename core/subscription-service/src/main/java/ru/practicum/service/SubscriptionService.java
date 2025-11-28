package ru.practicum.service;

import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.params.PublicEventSearchParam;

import java.util.List;

public interface SubscriptionService {
    void createSubscribe(long followerUserId, long followedToUserId);

    void deleteSubscribe(long followerUserId, long followedToUserId);

    List<UserShortDto> getSubscribes(long userId, int from, int size);

    List<EventShortDto> getEventsFeed(long userId, PublicEventSearchParam param);
}
