package ru.practicum.analyzer.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.analyzer.entity.EventSimilarity;
import ru.practicum.analyzer.entity.UserAction;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class AnalyzerMapper {
    public static EventSimilarity toSimilarityFromAvro(EventsSimilarityAvro avro) {
        return EventSimilarity.builder()
                .eventA(avro.getEventA())
                .eventB(avro.getEventB())
                .score(avro.getScore())
                .timestamp(avro.getTimestamp())
                .build();

    }

    public static UserAction toActionFromAvro(UserActionAvro avro) {
        return UserAction.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .actionType(avro.getActionType())
                .timestamp(avro.getTimestamp())
                .build();
    }
}
