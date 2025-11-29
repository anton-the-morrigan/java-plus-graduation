package ru.practicum.stats.analyzer.service.recommendation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.stats.analyzer.dal.entity.Interaction;
import ru.practicum.stats.analyzer.dal.entity.Similarity;
import ru.practicum.stats.analyzer.dal.repository.InteractionRepository;
import ru.practicum.stats.analyzer.dal.repository.SimilarityRepository;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    private final SimilarityRepository similarityRepository;
    private final InteractionRepository interactionRepository;

    @Override
    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        long limit = request.getMaxResults();

        Set<Long> allInteractionEvents = interactionRepository.findEventIdsForUserInteractions(request.getUserId());
        if (allInteractionEvents.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> lastInteractionEvents = allInteractionEvents.stream()
                .limit(limit)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Similarity> similaritiesLastInteractions = similarityRepository.findByEvent(lastInteractionEvents);

        Set<Long> recommendedEvents = similaritiesLastInteractions.stream().filter(s -> {
                    Long otherEvent = lastInteractionEvents.contains(s.getEvent1()) ? s.getEvent2() : s.getEvent1();
                    return !allInteractionEvents.contains(otherEvent);
                }).limit(limit)
                .map(s -> lastInteractionEvents.contains(s.getEvent1()) ? s.getEvent2() : s.getEvent1())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<RecommendedEventProto> result = new ArrayList<>(recommendedEvents.size());
        for (Long recommendedEvent : recommendedEvents) {
            Map<Long, Similarity> similarEventsWithInteractions = findSimilarityEvents(recommendedEvent).stream()
                    .filter(s -> allInteractionEvents.contains(s.getEvent2()))
                    .collect(Collectors.toMap(Similarity::getEvent2, Function.identity()));

            Map<Long, Interaction> interactions = interactionRepository.findByEventIdInAndUserId(similarEventsWithInteractions.keySet(), request.getUserId())
                    .stream()
                    .collect(Collectors.toMap(Interaction::getEventId, Function.identity()));

            double weightedEstimatesSum = 0.0;
            double similarityCoefficientsSum = 0.0;
            for (Long event : similarEventsWithInteractions.keySet()) {
                Double similarityCoefficient = similarEventsWithInteractions.get(event).getSimilarity();
                similarityCoefficientsSum += similarityCoefficient;
                weightedEstimatesSum += interactions.get(event).getRating() * similarityCoefficient;
            }
            double score = weightedEstimatesSum / similarityCoefficientsSum;
            result.add(RecommendedEventProto.newBuilder()
                    .setEventId(recommendedEvent)
                    .setScore(score)
                    .build());
        }
        return result;
    }

    @Override
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {

        Set<Long> interactions = interactionRepository.findEventIdsForUserInteractions(request.getUserId());
        List<Similarity> similarities = findSimilarityEvents(request.getEventId());

        return similarities.stream()
                .filter(s -> interactions.contains(s.getEvent2()))
                .sorted()
                .map(s ->
                        RecommendedEventProto.newBuilder()
                                .setEventId(s.getEvent2())
                                .setScore(s.getSimilarity())
                                .build()
                ).toList();
    }

    @Override
    public List<RecommendedEventProto> getEventsRating(InteractionsCountRequestProto request) {
        Map<Long, Double> ratings = new HashMap<>(request.getEventIdCount());

        List<Interaction> interactions = interactionRepository.findByEventIdIn(request.getEventIdList());
        for (Interaction interaction : interactions) {
            Long event = interaction.getEventId();
            ratings.compute(event, (k, v) -> v == null ? interaction.getRating() : v + interaction.getRating());
        }

        List<RecommendedEventProto> result = ratings.entrySet().stream()
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .toList();
        return result;
    }

    private List<Similarity> findSimilarityEvents(Long targetEvent) {
        List<Similarity> similarities = similarityRepository.findByEvent(targetEvent);

        return similarities.stream()
                .map(s -> {
                    if (Objects.equals(s.getEvent2(), targetEvent)) {
                        s.setEvent2(s.getEvent1());
                        s.setEvent1(targetEvent);
                    }
                    return s;
                }).toList();
    }
}
