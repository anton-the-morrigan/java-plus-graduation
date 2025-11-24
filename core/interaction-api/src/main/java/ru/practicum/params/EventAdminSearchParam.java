package ru.practicum.params;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import ru.practicum.dto.event.EventState;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventAdminSearchParam {
    List<Long> users;

    List<EventState> states;

    LocalDateTime rangeStart;

    LocalDateTime rangeEnd;

    List<Long> categories;

    Integer from;

    Integer size;

    public Pageable getPageable() {
        int page = from / size;
        return PageRequest.of(page, size);
    }
}
