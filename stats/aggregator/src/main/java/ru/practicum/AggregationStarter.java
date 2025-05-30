package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.config.AggregatorConfig;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private final AggregatorConfig aggregatorConfig;
    private final KafkaConsumer<String, UserActionAvro> consumer;
    private final KafkaProducer<String, SpecificRecordBase> producer;

    // Храним максимальные веса действий пользователей для каждого мероприятия
    private final Map<Long, Map<Long, Double>> userEventWeights = new HashMap<>();
    // Храним суммы весов для каждого мероприятия
    private final Map<Long, Double> eventWeightSums = new HashMap<>();

    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    private static void manageOffsets(ConsumerRecord<String, UserActionAvro> record, int count,
                                      KafkaConsumer<String, UserActionAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    public void start() {
        try {
            consumer.subscribe(aggregatorConfig.getConsumerTopic());
            while (true) {
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(aggregatorConfig.getConsumeAttemptTimeout());
                int count = 0;
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    processEvent(record.value());
                    manageOffsets(record, count, consumer);
                    count++;
                }
                consumer.commitAsync();
            }

        } catch (WakeupException ignored) {

        } catch (
                Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {

            try {
                producer.flush();
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }

    }

    private void processEvent(UserActionAvro userActionAvro) {
        long userId = userActionAvro.getUserId();
        long eventId = userActionAvro.getEventId();
        double weight = switch (userActionAvro.getActionType()) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
        double oldWeight = userEventWeights.computeIfAbsent(eventId, e -> new HashMap<>())
                .getOrDefault(userId, 0.0);
        if (oldWeight >= weight) {
            return; //вес не изменился - пересчитывать нечего
        }
        userEventWeights.computeIfAbsent(eventId, e -> new HashMap<>())
                .merge(userId, weight, Math::max);

        double newTotalWeight = countNewTotalWeightAndUpdateSaved(eventId, oldWeight, weight);
        for (Long eventBId : userEventWeights.keySet()) {
            if (eventBId == eventId || !userEventWeights.get(eventBId).containsKey(userId)) {
                continue;//пропускаем текущий ивент и те ивенты где юзер ничего не делал
            }
            double eventBWeight = userEventWeights.get(eventBId).get(userId);
            double oldMin = Math.min(oldWeight, eventBWeight);
            double newMin = Math.min(weight, eventBWeight);
            double delta = newMin - oldMin;
            double oldSmin = getSMin(eventBId, eventId);
            double minWeightSum = oldSmin;
            if (delta != 0) {
                double newMinSum = oldSmin + delta;
                minWeightSum = newMinSum;
                putSMin(eventBId, eventId, newMinSum);
            }
            double otherEventWeight = eventWeightSums.getOrDefault(eventBId, 0.0);
            double similarity = calculateSimilarity(minWeightSum, newTotalWeight, otherEventWeight);
            sendSimilarity(eventId, eventBId, similarity, userActionAvro.getTimestamp());
        }
    }

    private double countNewTotalWeightAndUpdateSaved(long eventId, double oldWeight, double newWeight) {
        double oldTotalWeight = eventWeightSums.getOrDefault(eventId, 0.0);
        double delta = newWeight - oldWeight;
        double newTotalWeight = oldTotalWeight + delta;
        eventWeightSums.put(eventId, newTotalWeight);
        return newTotalWeight;
    }


    public void putSMin(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    public double getSMin(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    private double calculateSimilarity(
            double minWeightSum,
            double totalEventWeight,
            double totalOtherEventWeight) {
        double denominator = Math.sqrt(totalEventWeight) * Math.sqrt(totalOtherEventWeight);
        return minWeightSum / denominator;
    }

    private void sendSimilarity(long eventA, long eventB, double similarity, Instant timestamp) {
        EventSimilarityAvro eventSimilarity = EventSimilarityAvro.newBuilder()
                .setEventA(Math.min(eventA, eventB))
                .setEventB(Math.max(eventA, eventB))
                .setScore(similarity)
                .setTimestamp(timestamp)
                .build();

        producer.send(new ProducerRecord<>(aggregatorConfig.getProducerTopic(), eventSimilarity));
    }

    public void stop() {
        consumer.wakeup();
    }
}
