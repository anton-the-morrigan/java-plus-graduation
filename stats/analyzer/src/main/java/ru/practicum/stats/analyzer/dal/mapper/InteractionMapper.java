package ru.practicum.stats.analyzer.dal.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.stats.analyzer.dal.entity.Interaction;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.Map;

@UtilityClass
public class InteractionMapper {
    private final Map<ActionTypeAvro, Float> actionTypeWeights =  Map.of(
            ActionTypeAvro.VIEW, 0.4f,
            ActionTypeAvro.REGISTER, 0.8f,
            ActionTypeAvro.LIKE, 1.0f
    );

    public static Interaction fromAvro(UserActionAvro avro) {
        return Interaction.builder()
                .userId(avro.getUserId())
                .eventId(avro.getEventId())
                .rating(actionTypeWeights.get(avro.getActionType()))
                .timestamp(avro.getTimestamp())
                .build();
    }
}
