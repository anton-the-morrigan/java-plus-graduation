package ru.practicum.stats.analyzer.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.stats.analyzer.service.recommendation.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class AnalyzerController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<RecommendedEventProto> recommendations = recommendationService.getRecommendationsForUser(request);
            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)));
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<RecommendedEventProto> recommendations = recommendationService.getSimilarEvents(request);
            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            List<RecommendedEventProto> recommendations = recommendationService.getEventsRating(request);
            recommendations.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)));
        }
    }
}
