package ru.practicum.entity.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventState;

@Component
public class StringToEventStateConverter implements Converter<String, EventState> {
    @Override
    public EventState convert(String source) {
        if (source == null) {
            return null;
        }
        return EventState.valueOf(source.trim().toUpperCase());
    }
}
