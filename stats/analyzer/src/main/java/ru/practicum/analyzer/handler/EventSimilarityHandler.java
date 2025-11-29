package ru.practicum.analyzer.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.analyzer.entity.EventSimilarity;
import ru.practicum.analyzer.mapper.AnalyzerMapper;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

@Service
@RequiredArgsConstructor
public class EventSimilarityHandler {
    private final EventSimilarityRepository eventSimilarityRepository;

    public EventSimilarity processEventSimilarity(EventsSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarity = AnalyzerMapper.toSimilarityFromAvro(eventSimilarityAvro);
        eventSimilarityRepository.save(eventSimilarity);
        return AnalyzerMapper.toSimilarityFromAvro(eventSimilarityAvro);
    }
}
