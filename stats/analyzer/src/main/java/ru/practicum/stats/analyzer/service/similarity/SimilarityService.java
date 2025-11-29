package ru.practicum.stats.analyzer.service.similarity;

import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

public interface SimilarityService {
    void handleSimilarity(EventsSimilarityAvro avro);
}
