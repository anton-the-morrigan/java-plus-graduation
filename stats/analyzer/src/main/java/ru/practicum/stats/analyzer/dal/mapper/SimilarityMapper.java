package ru.practicum.stats.analyzer.dal.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.stats.analyzer.dal.entity.Similarity;

import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

@UtilityClass
public class SimilarityMapper {
    public static Similarity fromAvro(EventsSimilarityAvro avro) {
        return Similarity.builder()
                .event1(avro.getEventA())
                .event2(avro.getEventB())
                .similarity(avro.getScore())
                .timestamp(avro.getTimestamp())
                .build();
    }
}
