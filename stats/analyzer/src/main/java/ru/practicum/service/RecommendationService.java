package ru.practicum.service;

import io.grpc.stub.StreamObserver;
import ru.practicum.ewm.stats.grpc.predict.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.predict.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.predict.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.predict.UserPredictionsRequestProto;

public interface RecommendationService {
    void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver);

    void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver);

    void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver);
}
