package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.EventClient;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.repository.SubscriptionRepository;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Subscription;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.service.SubscriptionService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestClient requestClient;

    private final String startDate = "2000-01-01 00:00:00";
    private final String endDate = "2100-01-01 00:00:00";

    @Override
    @Transactional
    public void createSubscribe(long followerUserId, long followedToUserId) {
        log.debug("Запрос на создание подписки пользователя id = {} на пользователя id = {}", followerUserId, followedToUserId);
        Subscription subscription = new Subscription(followerUserId, followedToUserId);
        subscriptionRepository.save(subscription);
        log.info("Создана подписка пользователя id = {} на пользователя id = {}", followerUserId, followedToUserId);
    }

    @Override
    @Transactional
    public void deleteSubscribe(long followerUserId, long followedToUserId) {
        log.debug("Запрос на удаление подписки пользователя id = {} на пользователя id = {}", followerUserId, followedToUserId);
        subscriptionRepository.deleteByFollowerAndFollowedTo(followerUserId, followedToUserId);
        log.info("Удалена подписка пользователя id = {} на пользователя id = {}", followerUserId, followedToUserId);
    }

    @Override
    public List<UserShortDto> getSubscribes(long userId, int from, int size) {
        log.debug("Запрос на получение подписок пользователя id = {}", userId);
        List<Long> followedUsersIds = subscriptionRepository.findFollowedUsersIds(userId);
        List<UserDto> users = userClient.getUsers(followedUsersIds, from, size);
        return users.stream().map(user -> new UserShortDto(user.getId(), user.getName())).toList();
    }

    @Override
    public List<EventShortDto> getEventsFeed(long userId, PublicEventSearchParam param) {
        log.debug("Запрос на получение ленты событий пользователя id = {}, params: {}", userId, param);
        List<Long> followedUsersIds = subscriptionRepository.findFollowedUsersIds(userId);

        List<EventShortDto> events = eventClient.getEventsFeedCogList(followedUsersIds, param);

        Map<Long, EventFullDto> eventsMap = eventClient.getEventsFeedCogMap(followedUsersIds, param);

        connectConfirmedRequests(events);
        if (param.getOnlyAvailable()) {
            events = events.stream().filter(event -> {
                Integer limit = eventsMap.get(event.getId()).getParticipantLimit();
                Long participants = event.getConfirmedRequests();
                return participants < limit;
            }).toList();
        }
        return events;
    }

    private void connectConfirmedRequests(Collection<EventShortDto> events) {
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(
                events.stream().map(EventShortDto::getId).toList());

        events.forEach(event ->
                event.setConfirmedRequests(confirmed.getOrDefault(event.getId(), 0L)));
    }
}