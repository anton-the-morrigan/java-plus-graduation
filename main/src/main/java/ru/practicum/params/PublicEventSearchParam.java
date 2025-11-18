package ru.practicum.params;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicEventSearchParam {
    String text;
    List<Long> categories;
    Boolean paid;
    LocalDateTime rangeStart;
    LocalDateTime rangeEnd;
    Boolean onlyAvailable;
    SortSearchParam sort;
    Integer from;
    Integer size;

    public Pageable getPageable() {
        int page = from / size;
        if (sort == SortSearchParam.EVENT_DATE) {
            return PageRequest.of(page, size, Sort.by("eventDate"));
        } else {
            return PageRequest.of(page, size);
        }
    }
}
