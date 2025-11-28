package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.entity.Compilation;
import ru.practicum.entity.Event;
import ru.practicum.repository.EventRepository;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CompilationMapper {
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public Compilation toCompilation(NewCompilationDto newCompilationDto) {
        Compilation compilation = new Compilation(null, newCompilationDto.getTitle(), newCompilationDto.getPinned(), null);
        if (newCompilationDto.getEvents() != null) {
            compilation.setEvents(eventIdsToEvents(newCompilationDto.getEvents()));
        } else {
            compilation.setEvents(new LinkedHashSet<>());
        }
        return compilation;
    }

    public CompilationDto toCompilationDto(Compilation compilation) {
        CompilationDto compilationDto = new CompilationDto(compilation.getId(), compilation.getTitle(), compilation.getPinned(), null);
        if (compilation.getEvents() != null) {
            compilationDto.setEvents(eventSetToEventShortDtoSet(compilation.getEvents()));
        } else {
            compilationDto.setEvents(new LinkedHashSet<>());
        }
        return compilationDto;
    }

    public Set<Event> eventIdsToEvents(Set<Long> eventIds) {
        Set<Event> events = new LinkedHashSet<>();
        for (Long eventId : eventIds) {
            Event event = eventRepository.findById(eventId).get();
            events.add(event);
        }
        return events;
    }

    private Set<EventShortDto> eventSetToEventShortDtoSet(Set<Event> events) {
        Set<EventShortDto> eventShortDtos = new LinkedHashSet<>();
        for (Event event : events) {
            eventShortDtos.add(eventMapper.toShortDto(event));
        }
        return eventShortDtos;
    }
}
