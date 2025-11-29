package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.AnalyzerClient;
import ru.practicum.client.RequestClient;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.request.EventRequestStatusUpdateRequest;
import ru.practicum.dto.request.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.entity.*;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.exception.BadRequestException;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final AnalyzerClient analyzerClient;

    @Override
    public List<EventFullDto> getEventsByParams(EventAdminSearchParam params) {
        log.debug("Get events by params: {}", params);

        Page<Event> searched = eventRepository.findAll(eventAdminSearchParamSpec(params), params.getPageable());

        List<Long> eventIds = searched.stream()
                .limit(params.getSize())
                .map(Event::getId)
                .toList();

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(eventIds);
        return searched.get()
                .map(event -> eventMapper.toFullDto(event,
                        ratings.getOrDefault(event.getId(), 0d),
                        confirmed.getOrDefault(event.getId(), 0L)))
                .toList();
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

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(List.of(eventId));
        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(List.of(eventId));

        return eventMapper.toFullDto(event,
                ratings.getOrDefault(eventId, 0d),
                confirmedRequests.getOrDefault(eventId, 0L));
    }

    @Override
    public EventFullDto getEventById(Long id, Long userId) {
        log.info("Get event: {}", id);

        Event event = eventRepository.findByIdAndState(id, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Событие не найдено или не опубликовано"));
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(event.getId()));
        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(List.of(id));

        return eventMapper.toFullDto(event,
                ratings.getOrDefault(id, 0d),
                confirmed.getOrDefault(id, 0L));
    }

    @Override
    public EventFullDto getEventForRequest(Long id) {
        log.info("Get event: {}", id);

        Event event = eventRepository.findById(id).get();
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(List.of(event.getId()));
        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(List.of(id));

        return eventMapper.toFullDto(event,
                ratings.getOrDefault(id, 0d),
                confirmed.getOrDefault(id, 0L));
    }

    @Override
    public List<EventShortDto> searchEvents(PublicEventSearchParam param) {
        log.info("Search events: {}", param);

        Page<Event> events = eventRepository.findAll(eventPublicSearchParamSpec(param), param.getPageable());

        List<Long> eventIds = events.stream()
                .map(Event::getId)
                .toList();

        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);
        Map<Long, Long> confirmed = requestClient.getConfirmedRequestsCount(eventIds);

        Stream<EventShortDto> eventShortDtoStream = events.stream()
                .map(event -> {
                    if (param.getOnlyAvailable() && confirmed.get(event.getId()) >= event.getParticipantLimit()) {
                        return null;
                    }
                    return eventMapper.toShortDto(event,
                            ratings.getOrDefault(event.getId(), 0d),
                            confirmed.getOrDefault(event.getId(), 0L));
                })
                .filter(Objects::nonNull);

        if (param.getSort() == SortSearchParam.RATING) {
            eventShortDtoStream = eventShortDtoStream.sorted(Comparator.comparingDouble(EventShortDto::getRating));
        }
        return eventShortDtoStream.toList();
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
        Map<Long, Double> recommended = analyzerClient.getRecommendationsForUser(userId, 100);
        if (recommended.isEmpty()) {
            return List.of();
        }
        List<Long> eventIds = new ArrayList<>(recommended.keySet());

        List<Event> events = eventRepository.findAllById(eventIds);
        Map<Long, Double> ratings = analyzerClient.getInteractionsCount(eventIds);
        Map<Long, Long> confirmedRequests = requestClient.getConfirmedRequestsCount(eventIds);

        return events.stream()
                .map(event -> eventMapper.toShortDto(event,
                        ratings.getOrDefault(event.getId(), 0d),
                        confirmedRequests.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    @Override
    public void likeEvent(Long eventId, Long userId) {
        Optional<ParticipationRequestDto> request = requestClient.getRequestForEvent(eventId).stream()
                .filter(r -> r.getRequester().equals(userId))
                .findFirst();

        boolean requestIsConfirmed = request.isPresent() && RequestStatus.CONFIRMED == request.get().getStatus();

        if (!requestIsConfirmed) {
            throw new BadRequestException(
                    String.format("Event %s cannot be liked by user %s. The user is not a participant in the event.", eventId, userId));
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
}