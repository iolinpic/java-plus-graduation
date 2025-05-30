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
        userActionRepository.save(userAction);
    }

    @Override
    public void eventsSimilarityReceived(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarity = EventSimilarity.builder()
                .eventA(eventSimilarityAvro.getEventA())
                .eventB(eventSimilarityAvro.getEventB())
                .score(eventSimilarityAvro.getScore())
                .timestamp(mapTimestamp(eventSimilarityAvro.getTimestamp()))
                .build();
        eventSimilarityRepository.save(eventSimilarity);
    }

    private LocalDateTime mapTimestamp(Instant instant) {
        return instant.atZone(ZoneId.of("UTC")).toLocalDateTime();
    }
}
