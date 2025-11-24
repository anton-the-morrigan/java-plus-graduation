package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.entity.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.params.EventAdminSearchParam;
import ru.practicum.params.EventUserSearchParam;
import ru.practicum.params.PublicEventSearchParam;
import ru.practicum.params.SortSearchParam;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final ParticipationRequestRepository requestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final StatsClient statsClient;

    @Override
    public List<EventFullDto> getEventsByParams(EventAdminSearchParam params) {
        log.debug("Get events by params: {}", params);

        Page<Event> searched = eventRepository.findAll(eventAdminSearchParamSpec(params), params.getPageable());

        List<Long> eventIds = searched.stream()
                .limit(params.getSize())
                .map(Event::getId)
                .toList();

        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(eventIds,
                RequestStatus.CONFIRMED);
        return searched.stream()
                .limit(params.getSize())
                .map(event -> {
                    EventFullDto dto = eventMapper.toFullDto(event);
                    dto.setConfirmedRequests(confirmed.get(dto.getId()) == null ? 0 : confirmed.get(dto.getId()));
                    dto.setViews(views.get(event.getId()) == null ? 0 : views.get(event.getId()));
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

        Map<Long, Long> views = getViews(List.of(eventId));
        Map<Long, Long> confirmedRequests = requestRepository.countRequestsByEventIdsAndStatus(List.of(eventId),
                RequestStatus.CONFIRMED);

        dto.setViews(views.get(eventId));
        dto.setConfirmedRequests(confirmedRequests.get(eventId));

        return dto;
    }

    @Override
    public EventFullDto getEventById(Long id) {
        log.info("Get event: {}", id);

        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));
        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(List.of(event.getId()), RequestStatus.CONFIRMED);
        Map<Long, Long> views = getViews(List.of(event.getId()));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.get(dto.getId()));
        dto.setViews(views.get(dto.getId()));
        return dto;
    }

    @Override
    public List<EventShortDto> searchEvents(PublicEventSearchParam param) {
        log.info("Search events: {}", param);

        Page<Event> events = eventRepository.findAll(eventPublicSearchParamSpec(param), param.getPageable());

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();
        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(eventIds,
                RequestStatus.CONFIRMED);

        Stream<EventShortDto> eventShortDtoStream = events.stream()
                .map(event -> {
                    if (param.getOnlyAvailable() && confirmed.get(event.getId()) >= event.getParticipantLimit()) {
                        return null;
                    }
                    EventShortDto dto = eventMapper.toShortDto(event);
                    dto.setConfirmedRequests(confirmed.get(dto.getId()) == null ? 0 : confirmed.get(dto.getId()));
                    dto.setViews(views.get(event.getId()) == null ? 0 : views.get(dto.getId()));
                    return dto;
                })
                .filter(Objects::nonNull);
        if (param.getSort() == SortSearchParam.VIEWS) {
            return eventShortDtoStream
                    .sorted(Comparator.comparingLong(EventShortDto::getViews))
                    .toList();
        } else {
            return eventShortDtoStream
                    .toList();
        }
    }

    @Override
    public List<EventShortDto> getUsersEvents(EventUserSearchParam param) {
        log.info("Get users events: {}", param);
        Page<Event> events = eventRepository.findByInitiatorId(param.getUserId(), param.getPageable());

        List<Long> eventIds = events.stream().map(Event::getId).toList();
        Map<Long, Long> views = getViews(eventIds);
        Map<Long, Long> confirmedRequests = requestRepository.countRequestsByEventIdsAndStatus(eventIds,
                RequestStatus.CONFIRMED);

        return events.stream()
                .map(event -> {
                    EventShortDto shortDto = eventMapper.toShortDto(event);
                    shortDto.setViews(views.get(event.getId()));
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

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        event.setInitiator(initiator);
        event.setCategory(category);

        Event saved = eventRepository.saveAndFlush(event);

        EventFullDto dtoResponse = eventMapper.toFullDto(saved);
        dtoResponse.setViews(0L);
        dtoResponse.setConfirmedRequests(0L);

        return dtoResponse;
    }

    @Override
    public EventFullDto getEventByIdAndUserId(Long eventId, Long userId) {
        log.info("Get event: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));

        if (!Objects.equals(event.getInitiator().getId(), userId)) {
            throw new ConflictException("Событие добавленно не теущем пользователем");
        }

        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(List.of(event.getId()), RequestStatus.CONFIRMED);
        Map<Long, Long> views = getViews(List.of(event.getId()));

        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setConfirmedRequests(confirmed.get(dto.getId()));
        dto.setViews(views.get(dto.getId()));
        return dto;
    }

    @Override
    public EventFullDto updateEventByUser(Long eventId, Long userId, UpdateEventUserRequest event) {
        Event eventToUpdate = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено id=" + eventId));
        if (!Objects.equals(eventToUpdate.getInitiator().getId(), userId) ||
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

        Map<Long, Long> confirmed = requestRepository.countRequestsByEventIdsAndStatus(List.of(eventId), RequestStatus.CONFIRMED);
        Map<Long, Long> views = getViews(List.of(eventId));

        EventFullDto result = eventMapper.toFullDto(updated);
        result.setConfirmedRequests(confirmed.get(eventId));
        result.setViews(views.get(eventId));
        return result;
    }


    private Map<Long, Long> getViews(List<Long> eventIds) {
        List<ViewStatsDto> stats = statsClient.getStats(
                "2000-01-01 00:00:00",
                "2100-01-01 00:00:00",
                eventIds.stream().map(id -> "/events/" + id).toList(),
                true);
        return stats.stream()
                .filter(statsDto -> !statsDto.getUri().equals("/events"))
                .collect(toMap(statDto ->
                        Long.parseLong(statDto.getUri().replace("/events/", "")), ViewStatsDto::getHits));
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
}