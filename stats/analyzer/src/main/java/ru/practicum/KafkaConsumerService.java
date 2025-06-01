package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.config.AnalyserConfig;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaConsumerService {
    private final ru.practicum.service.KafkaConsumerService kafkaConsumerService;
    private static final Map<TopicPartition, OffsetAndMetadata> currentSimilarityOffsets = new HashMap<>();
    private static final Map<TopicPartition, OffsetAndMetadata> currentActionOffsets = new HashMap<>();
    private final AnalyserConfig analyserConfig;
    private final KafkaConsumer<String, EventSimilarityAvro> consumerSimilarity;
    private final KafkaConsumer<String, UserActionAvro> consumerUserAction;

    private static void manageSimilarityOffsets(ConsumerRecord<String, EventSimilarityAvro> record, int count,
                                                KafkaConsumer<String, EventSimilarityAvro> consumer) {
        currentSimilarityOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentSimilarityOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private static void manageActionOffsets(ConsumerRecord<String, UserActionAvro> record, int count,
                                            KafkaConsumer<String, UserActionAvro> consumer) {
        currentActionOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentActionOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    public void start() {
        try {
            consumerSimilarity.subscribe(analyserConfig.getSimilarityTopics());
            consumerUserAction.subscribe(analyserConfig.getUserTopics());
            while (true) {
                ConsumerRecords<String, EventSimilarityAvro> records = consumerSimilarity.poll(analyserConfig.getConsumeAttemptTimeout());
                int countSimilarity = 0;
                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    kafkaConsumerService.eventsSimilarityReceived(record.value());
                    manageSimilarityOffsets(record, countSimilarity, consumerSimilarity);
                    countSimilarity++;
                }
                consumerSimilarity.commitAsync();

                ConsumerRecords<String, UserActionAvro> actionRecords = consumerUserAction.poll(analyserConfig.getConsumeAttemptTimeout());
                int countAction = 0;
                for (ConsumerRecord<String, UserActionAvro> record : actionRecords) {
                    kafkaConsumerService.userActionReceived(record.value());
                    manageActionOffsets(record, countAction, consumerUserAction);
                    countAction++;
                }
                consumerSimilarity.commitAsync();
            }

        } catch (WakeupException ignored) {

        } catch (
                Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {

            try {
                consumerSimilarity.commitSync(currentSimilarityOffsets);
                consumerUserAction.commitSync(currentActionOffsets);
            } finally {
                log.info("Закрываем консьюмер");
                consumerSimilarity.close();
                consumerUserAction.close();
            }
        }

    }


    public void stop() {
        consumerSimilarity.wakeup();
        consumerUserAction.wakeup();
    }
}
