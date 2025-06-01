package ru.practicum.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.grpc.analizer.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.grpc.predict.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.predict.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.predict.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.predict.UserPredictionsRequestProto;
import ru.practicum.service.RecommendationService;

@GrpcService
@RequiredArgsConstructor
public class RecommendationController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver
    ) {
        recommendationService.getSimilarEvents(request, responseObserver);
    }

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver
    ) {
        recommendationService.getRecommendationsForUser(request, responseObserver);
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver
    ) {
        recommendationService.getInteractionsCount(request, responseObserver);
    }
}
