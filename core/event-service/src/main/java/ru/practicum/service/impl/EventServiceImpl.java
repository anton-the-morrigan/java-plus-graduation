package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.client.RequestClient;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.*;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.params.EventAdminSearchParam;
import ru.practicum.params.EventUserSearchParam;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.params.SortSearchParam;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.service.EventService;
import ru.practicum.specifications.EventSpecifications;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static ru.practicum.specifications.EventSpecifications.eventAdminSearchParamSpec;
import static ru.practicum.specifications.EventSpecifications.eventPublicSearchParamSpec;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final RequestClient requestClient;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final StatsClient statsClient;

    @Override
    public List<EventFullDto> getEventsByParams(EventAdminSearchParam params) {
        log.debug("Get events by params: {}", params);

        Page<Event> searched = eventRepository.findAll(eventAdminSearchParamSpec(params), params.getPageable());

        List<Long> eventIds = searched.stream()
                .limit(params.getSize())
                .map(Event::getId)
                .toList();

        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(eventIds);
        return searched.stream()
                .limit(params.getSize())
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setConfirmedRequests(confirmed.get(dto.getId()) == null ? 0 : confirmed.get(dto.getId()));
                    return dto;
                })
                .collect(toList());
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Update event: {}", updateRequest);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event id=" + eventId + "not found"));

        if (event.getState() != EventState.PENDING && updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT) {
            throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
        }
        if (event.getState() == EventState.PUBLISHED && updateRequest.getStateAction() == AdminEventAction.REJECT_EVENT) {
            throw new ConflictException("Cannot reject the event because it's not in the right state: PUBLISHED");
        }
        if (event.getEventDate().minusHours(1).isBefore(LocalDateTime.now())) {
            throw new ConflictException("To late to change event");
        }

        updateNotNullFields(event, updateRequest);
        event.setState(updateRequest.getStateAction() == AdminEventAction.PUBLISH_EVENT ? EventState.PUBLISHED : EventState.CANCELED);
        event.setPublishedOn(LocalDateTime.now());
        Event updated = eventRepository.save(event);

        EventFullDto dto = eventMapper.toFullDto(updated);

        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(List.of(eventId));

        dto.setConfirmedRequests(confirmedRequests.get(eventId));

        return dto;
    }

    @Override
    public EventFullDto getEventById(Long id, Long userId) {
        log.info("Get event: {}", id);

        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(event.getId()));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.get(dto.getId()));

        Map<Long, Double> eventRating = getEventRating(List.of(event));
        double rating = eventRating.getOrDefault(event.getId(), 0.0);
        dto.setRating(rating);

        if (userId != null) {
            statsClient.sendUserAction(userId, id, ActionTypeProto.ACTION_VIEW);
        }

        return dto;
    }

    @Override
    public EventFullDto getEventForRequest(Long id) {
        log.info("Get event: {}", id);

        Event event = eventRepository.findById(id).get();
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(event.getId()));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.get(dto.getId()));
        return dto;
    }

    @Override
    public List<EventShortDto> searchEvents(PublicEventSearchParam param) {
        log.info("Search events: {}", param);

        Page<Event> events = eventRepository.findAll(eventPublicSearchParamSpec(param), param.getPageable());

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(eventIds);

        Stream<EventShortDto> eventShortDtoStream = events.stream()
                .map(event -> {
                    if (param.getOnlyAvailable() && confirmed.get(event.getId()) >= event.getParticipantLimit()) {
                        return null;
                    }
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setConfirmedRequests(confirmed.get(dto.getId()) == null ? 0 : confirmed.get(dto.getId()));
                    return dto;
                })
                .filter(Objects::nonNull);
        return eventShortDtoStream
                .toList();
    }

    @Override
    public List<EventShortDto> getUsersEvents(EventUserSearchParam param) {
        log.info("Get users events: {}", param);
        Page<Event> events = eventRepository.findByInitiator(param.getUserId(), param.getPageable());

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(eventIds);

        return events.stream()
                .map(event -> {
                    EventShortDto shortDto = eventMapper.toShortDto(event);
                    shortDto.setConfirmedRequests(confirmedRequests.get(event.getId()));
                    return shortDto;
                })
                .toList();

    }

    @Override
    @Transactional
    public EventFullDto saveEvent(NewEventDto dto, Long userId) {
        log.info("Save event: {}", dto);

        Event event = eventMapper.toEntity(dto, userId);

        Long categoryId = dto.getCategory().longValue();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Категория с id=" + categoryId + " не найдена"));

        event.setInitiator(userId);
        event.setCategory(category);

        Event saved = eventRepository.saveAndFlush(event);

        EventFullDto dtoResponse = eventMapper.toFullDto(saved);
        dtoResponse.setConfirmedRequests(0L);

        return dtoResponse;
    }

    @Override
    public EventFullDto getEventByIdAndUserId(Long eventId, Long userId) {
        log.info("Get event: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!Objects.equals(event.getInitiator(), userId)) {
            throw new ConflictException("Событие добавленно не теущем пользователем");
        }

        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(event.getId()));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.get(dto.getId()));
        return dto;
    }

    @Override
    public EventFullDto updateEventByUser(Long eventId, Long userId, UpdateEventUserRequest event) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено id=" + eventId));
        if (!Objects.equals(eventToUpdate.getInitiator(), userId) ||
                eventToUpdate.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Событие добавленно не теущем пользователем или уже было опубликовано");
        }
        updateNotNullFields(eventToUpdate, event);
        if (event.getStateAction() == UserEventAction.CANCEL_REVIEW) {
            eventToUpdate.setState(EventState.CANCELED);
        } else if (event.getStateAction() == UserEventAction.SEND_TO_REVIEW) {
            eventToUpdate.setState(EventState.PENDING);
        }
        Event updated = eventRepository.save(eventToUpdate);

        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(eventId));

        EventFullDto result = eventMapper.toFullDto(updated);
        result.setConfirmedRequests(confirmed.get(eventId));
        return result;
    }

    @Override
    public List<EventShortDto> getEventsFeedCogList(List<Long> followedUsersIds, PublicEventSearchParam param) {
        Page<Event> eventsPage = eventRepository.findAll(EventSpecifications.eventFeedSearchParamSpec(followedUsersIds, param), param.getPageable());
        return eventMapper.toShortDto(eventsPage.toList());
    }

    @Override
    public Map<Long, EventFullDto> getEventsFeedCogMap(List<Long> followedUsersIds, PublicEventSearchParam param) {
        Page<EventFullDto> eventsPage = eventRepository.findAll(EventSpecifications.eventFeedSearchParamSpec(followedUsersIds, param), param.getPageable()).map(eventMapper::toFullDto);
        return eventsPage.stream().collect(Collectors.toMap(EventFullDto::getId, Function.identity()));
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId) {
        Map<Long, Double> eventRating  = statsClient.getRecommendationsForUser(userId, 10)
                .collect(
                        Collectors.toMap(RecommendedEventProto::getEventId, RecommendedEventProto::getScore)
                );

        List<Event> events =  eventRepository.findByIdIn(eventRating.keySet()).stream()
                .map(event -> {
                    double rating = eventRating.getOrDefault(event.getId(), 0.0);
                    event.setRating(rating);
                    return event;
                })
                .toList();

        return events.stream()
                .map(eventMapper::toShortDto)
                .toList();
    }

    @Override
    public void likeEvent(Long eventId, Long userId) {
        if (userId != null && eventId != null) {
            statsClient.sendUserAction(eventId, userId, ActionTypeProto.ACTION_LIKE);
        }
    }

    private void updateNotNullFields(Event eventToUpdate, UpdateEventUserRequest event) {
        if (event.getAnnotation() != null) eventToUpdate.setAnnotation(event.getAnnotation());
        if (event.getCategory() != null) eventToUpdate.setCategory(Category.builder().id(event.getCategory()).build());
        if (event.getDescription() != null) eventToUpdate.setDescription(event.getDescription());
        if (event.getEventDate() != null) eventToUpdate.setEventDate(event.getEventDate());
        if (event.getLocation() != null) {
            LocationDto locDto = event.getLocation();
            Location loc = new Location();
            loc.setLat(locDto.getLat());
            loc.setLon(locDto.getLon());
            eventToUpdate.setLocation(loc);
        }
        if (event.getPaid() != null) eventToUpdate.setPaid(event.getPaid());
        if (event.getParticipantLimit() != null) eventToUpdate.setParticipantLimit(event.getParticipantLimit());
        if (event.getRequestModeration() != null) eventToUpdate.setRequestModeration(event.getRequestModeration());
        if (event.getTitle() != null) eventToUpdate.setTitle(event.getTitle());
    }

    private void updateNotNullFields(Event eventToUpdate, UpdateEventAdminRequest event) {
        if (event.getAnnotation() != null) eventToUpdate.setAnnotation(event.getAnnotation());
        if (event.getCategory() != null) eventToUpdate.setCategory(Category.builder().id(event.getCategory()).build());
        if (event.getDescription() != null) eventToUpdate.setDescription(event.getDescription());
        if (event.getEventDate() != null) eventToUpdate.setEventDate(event.getEventDate());
        if (event.getLocation() != null) {
            LocationDto locDto = event.getLocation();
            Location loc = new Location();
            loc.setLat(locDto.getLat());
            loc.setLon(locDto.getLon());
            eventToUpdate.setLocation(loc);
        }
        if (event.getPaid() != null) eventToUpdate.setPaid(event.getPaid());
        if (event.getParticipantLimit() != null) eventToUpdate.setParticipantLimit(event.getParticipantLimit());
        if (event.getRequestModeration() != null) eventToUpdate.setRequestModeration(event.getRequestModeration());
        if (event.getTitle() != null) eventToUpdate.setTitle(event.getTitle());
    }

    private Map<Long, Double> getEventRating(List<Event> events) {
        log.debug("Ищем id для событий для поиска рейтинга");
        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        log.debug("Нашли id для событий, ищем рейтинг");
        Map<Long, Double> res = statsClient.getInteractionsCount(eventIds)
                .collect(Collectors.toMap(
                        RecommendedEventProto::getEventId,
                        RecommendedEventProto::getScore
                ));
        log.debug("Нашли рейтинг");
        return res;
    }
}