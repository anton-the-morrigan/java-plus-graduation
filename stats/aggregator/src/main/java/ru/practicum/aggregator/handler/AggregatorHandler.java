package ru.practicum.aggregator.handler;

import org.springframework.stereotype.Service;
import ru.practicum.avro.ActionWeights;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AggregatorHandler {

    private final Map<Long, Map<Long, Double>> eventUserWeight = new HashMap<>();
    private final Map<Long, Double> eventSumWeight = new HashMap<>();
    private final Map<Long, Map<Long, Double>> eventSumMinWeight = new HashMap<>();

    public List<EventsSimilarityAvro> updateSimilarity(UserActionAvro userActionAvro) {
        Long userId = userActionAvro.getUserId();
        Long eventId = userActionAvro.getEventId();
        ActionTypeAvro actionType = userActionAvro.getActionType();
        Double weight = ActionWeights.WEIGHTS.get(actionType);
        Instant timestamp = userActionAvro.getTimestamp();
        Map<Long, Double> userWeight = eventUserWeight.computeIfAbsent(eventId, k -> new HashMap<>());
        Double currentWeight = userWeight.get(userId);

        if (currentWeight == null || currentWeight < weight) {
            userWeight.put(userId, weight); // обновляем вес
            eventSumWeightUpdate(eventId);
            return eventSumMinWeightUpdate(eventId, userId, weight, currentWeight, timestamp);
        } else {
            return List.of();
        }
    }

    private List<EventsSimilarityAvro> eventSumMinWeightUpdate(Long eventId, Long userId, Double newWeight,
                                                               Double oldWeight, Instant timestamp) {
        List<EventsSimilarityAvro> eventSimilarityAvroList = new ArrayList<>();

        for (Long otherEventId : eventUserWeight.keySet()) { // проходимся по всем Event
            if (otherEventId.equals(eventId)) {
                continue;
            }
            long minId = Math.min(otherEventId, eventId);
            long maxId = Math.max(otherEventId, eventId);
            Map<Long, Double> usersOther = eventUserWeight.get(otherEventId);

            if (!usersOther.containsKey(userId)) {
                continue;
            }

            double otherWeight = usersOther.get(userId);
            double oldW = (oldWeight != null) ? oldWeight : 0.0;
            double oldMin = Math.min(oldW, otherWeight);  // используем именно минимум, чтобы не портить данные
            double newMin = Math.min(newWeight, otherWeight);
            double deltaMin = newMin - oldMin;

            if (deltaMin != 0) {
                eventSumMinWeight
                        .computeIfAbsent(minId, k -> new HashMap<>())
                        .merge(maxId, deltaMin, Double::sum);
            }

            double sumMin = eventSumMinWeight.get(minId).get(maxId);
            double sumA = eventSumWeight.get(minId);
            double sumB = eventSumWeight.get(maxId);
            double score = sumMin / Math.sqrt(sumA * sumB);

            EventsSimilarityAvro.Builder builder = EventsSimilarityAvro.newBuilder()
                    .setEventA(minId)
                    .setEventB(maxId)
                    .setScore(score)
                    .setTimestamp(timestamp);
            EventsSimilarityAvro EventsSimilarityAvro = builder.build();
            eventSimilarityAvroList.add(EventsSimilarityAvro);
        }

        return eventSimilarityAvroList;
    }

    private void eventSumWeightUpdate(Long eventId) {
        double sumWeight = eventUserWeight.get(eventId).values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        eventSumWeight.put(eventId, sumWeight);
    }
}
