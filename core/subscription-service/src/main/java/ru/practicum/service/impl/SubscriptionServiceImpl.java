package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.repository.SubscriptionRepository;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.RequestStatus;
import ru.practicum.entity.Subscription;
import ru.practicum.entity.User;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.UserMapperStruct;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.params.SortSearchParam;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.SubscriptionService;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.practicum.specifications.EventSpecifications.eventFeedSearchParamSpec;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final UserMapperStruct userMapper;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    private final String startDate = "2000-01-01 00:00:00";
    private final String endDate = "2100-01-01 00:00:00";

    @Override
    @Transactional
    public void createSubscribe(long followerUserId, long followedToUserId) {
        log.debug("Запрос на создание подписки пользователя id = {} на пользователя id = {}", followerUserId, followedToUserId);
        User follower = getUserOrElseThrow(followerUserId);
        User followedTo = getUserOrElseThrow(followedToUserId);
        Subscription subscription = new Subscription(follower, followedTo);
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
        List<User> users = userRepository.findAllByIdIn(followedUsersIds, PageRequest.of(from / size, size));
        return users.stream().map(userMapper::toShortDto).toList();
    }

    @Override
    public List<EventShortDto> getEventsFeed(long userId, PublicEventSearchParam param) {
        log.debug("Запрос на получение ленты событий пользователя id = {}, params: {}", userId, param);
        List<Long> followedUsersIds = subscriptionRepository.findFollowedUsersIds(userId);
        Page<Event> eventsPage = eventRepository.findAll(EventSpecifications.eventFeedSearchParamSpec(followedUsersIds, param), param.getPageable());
        Map<Long, Event> eventsMap = eventsPage.stream().collect(Collectors.toMap(Event::getId, Function.identity()));

        List<EventShortDto> events = eventMapper.toShortDto(eventsPage.toList());
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
        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(
                events.stream().map(EventShortDto::getId).toList(),
                RequestStatus.CONFIRMED);

        events.forEach(event ->
                event.setConfirmedRequests(confirmed.getOrDefault(event.getId(), 0L)));
    }

    private User getUserOrElseThrow(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь id = " + userId + " не существует"));
    }
}