package ru.practicum.service;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.grpc.predict.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.grpc.predict.RecommendedEventProto;
import ru.practicum.ewm.stats.grpc.predict.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.grpc.predict.UserPredictionsRequestProto;
import ru.practicum.models.EventSimilarity;
import ru.practicum.models.UserAction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionRepository userActionRepository;

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventAOrEventB(request.getEventId(), request.getEventId());

        Set<Long> userInteractions = userActionRepository.findByUserId(request.getUserId())
                .stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        similarities.stream()
                .filter(sim -> !userInteractions.contains(sim.getEventA()) || !userInteractions.contains(sim.getEventB()))
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(request.getMaxResults())
                .forEach(sim -> {
                    long recommendedEvent = sim.getEventA().equals(request.getEventId()) ? sim.getEventB() : sim.getEventA();
                    responseObserver.onNext(RecommendedEventProto.newBuilder()
                            .setEventId(recommendedEvent)
                            .setScore(sim.getScore())
                            .build());
                });

        responseObserver.onCompleted();
    }

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        List<UserAction> interactions = userActionRepository.findByUserId(request.getUserId());

        if (interactions.isEmpty()) {
            responseObserver.onCompleted();
            return;
        }

        Set<Long> recentEvents = interactions.stream()
                .sorted(Comparator.comparing(UserAction::getTimestamp).reversed())
                .limit(request.getMaxResults())
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        List<EventSimilarity> similarities = new ArrayList<>();
        for (Long eventId : recentEvents) {
            similarities.addAll(eventSimilarityRepository.findByEventAOrEventB(eventId, eventId));
        }

        Set<Long> userInteractions = interactions.stream().map(UserAction::getEventId).collect(Collectors.toSet());

        similarities.stream()
                .filter(sim -> !userInteractions.contains(sim.getEventA()) || !userInteractions.contains(sim.getEventB()))
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(request.getMaxResults())
                .forEach(sim -> {
                    long recommendedEvent = userInteractions.contains(sim.getEventA()) ? sim.getEventB() : sim.getEventA();
                    responseObserver.onNext(RecommendedEventProto.newBuilder()
                            .setEventId(recommendedEvent)
                            .setScore(sim.getScore())
                            .build());
                });

        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        for (long eventId : request.getEventIdList()) {
            double totalWeight = userActionRepository.findByEventId(eventId)
                    .stream()
                    .mapToDouble((uah) -> switch (uah.getActionType()) {
                        case VIEW -> 0.4;
                        case REGISTER -> 0.8;
                        case LIKE -> 1.0;
                    })
                    .sum();

            responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(eventId)
                    .setScore(totalWeight)
                    .build());
        }

        responseObserver.onCompleted();
    }
}
