package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.client.EventClient;
import ru.practicum.client.RequestClient;
import ru.practicum.client.UserClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.repository.SubscriptionRepository;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Subscription;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.params.SortSearchParam;
import ru.practicum.service.SubscriptionService;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserClient userClient;
    private final EventClient eventClient;
    private final RequestClient requestClient;
    private final StatsClient statsClient;

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
        subscriptionRepository.deleteByFollower_IdIsAndFollowedTo_Id(followerUserId, followedToUserId);
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

        connectViews(events);
        connectConfirmedRequests(events);
        if (param.getOnlyAvailable()) {
            events = events.stream().filter(event -> {
                Integer limit = eventsMap.get(event.getId()).getParticipantLimit();
                Long participants = event.getConfirmedRequests();
                return participants < limit;
            }).toList();
        }
        if (SortSearchParam.VIEWS.equals(param.getSort())) {
            events = events.stream().sorted(Comparator.comparingLong(EventShortDto::getViews).reversed()).toList();
        }
        return events;
    }

    private void connectViews(Collection<EventShortDto> events) {
        try {
            Map<Long, Long> views = statsClient.getStats(
                    startDate,
                    endDate,
                    events.stream().map(event -> "/events/" + event.getId()).toList(),
                    true)
                    .stream()
                    .filter(statDto -> statDto.getUri().matches("^/events/\\d+$"))
                    .collect(Collectors.toMap(statDto ->
                            Long.parseLong(statDto.getUri().replace("/events/", "")),
                            ViewStatsDto::getHits)
                    );

            events.forEach(event -> event.setViews(views.getOrDefault(event.getId(), 0L)));
        } catch (Exception e) {
            log.warn("Ошибка получения статистики просмотров", e);
        }
    }

    private void connectConfirmedRequests(Collection<EventShortDto> events) {
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(
                events.stream().map(EventShortDto::getId).toList());

        events.forEach(event ->
                event.setConfirmedRequests(confirmed.getOrDefault(event.getId(), 0L)));
    }
}