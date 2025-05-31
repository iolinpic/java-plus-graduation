package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.models.EventSimilarity;
import ru.practicum.models.UserAction;
import ru.practicum.models.UserActionType;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KafkaConsumerServiceImpl implements KafkaConsumerService {
    private final EventSimilarityRepository eventSimilarityRepository;
    private final UserActionRepository userActionRepository;

    @Override
    public void userActionReceived(UserActionAvro userActionAvro) {
        UserAction userAction = UserAction.builder()
                .userId(userActionAvro.getUserId())
                .eventId(userActionAvro.getEventId())
                .actionType(UserActionType.valueOf(userActionAvro.getActionType().name()))
                .timestamp(mapTimestamp(userActionAvro.getTimestamp()))
                .build();
        List<UserAction> actions = userActionRepository.findByUserIdAndEventId(userActionAvro.getUserId(), userActionAvro.getEventId());
        if (!actions.isEmpty()) {
            UserAction oldAction = actions.getFirst();
            if (calcInteractionScore(oldAction.getActionType()) < calcInteractionScore(userAction.getActionType())) {
                userActionRepository.delete(oldAction);
                userActionRepository.save(userAction);
            }
        } else {
            userActionRepository.save(userAction);
        }

    }

    private double calcInteractionScore(UserActionType type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }

    @Override
    public void eventsSimilarityReceived(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarity = EventSimilarity.builder()
                .eventA(eventSimilarityAvro.getEventA())
                .eventB(eventSimilarityAvro.getEventB())
                .score(eventSimilarityAvro.getScore())
                .timestamp(mapTimestamp(eventSimilarityAvro.getTimestamp()))
                .build();
        List<EventSimilarity> similarities = eventSimilarityRepository.findByEventAAndEventB(eventSimilarity.getEventA(), eventSimilarity.getEventB());
        if (!similarities.isEmpty()) {
            EventSimilarity oldSimilarity = similarities.getFirst();
            oldSimilarity.setScore(eventSimilarity.getScore());
            oldSimilarity.setTimestamp(eventSimilarity.getTimestamp());
            eventSimilarityRepository.save(oldSimilarity);
        }else {
            eventSimilarityRepository.save(eventSimilarity);
        }

    }

    private LocalDateTime mapTimestamp(Instant instant) {
        return instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
    }
}
