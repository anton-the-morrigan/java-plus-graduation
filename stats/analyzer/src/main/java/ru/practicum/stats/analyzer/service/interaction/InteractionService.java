package ru.practicum.stats.analyzer.service.interaction;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface InteractionService {
    void handleInteraction(UserActionAvro avro);
}
