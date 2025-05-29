package ru.practicum.handlers.users;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProducerConfig;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.grpc.user.UserActionProto;
import ru.practicum.mappers.TimestampMapper;

@Component
@RequiredArgsConstructor
public class UserActionsHandler {
    private final KafkaProducerConfig kafkaProducerConfig;
    private final KafkaProducer<String, SpecificRecordBase> producer;


    public void handle(UserActionProto actionProto) {
        producer.send(new ProducerRecord<>(kafkaProducerConfig.getTopic(), mapToAvro(actionProto)));
    }

    private UserActionAvro mapToAvro(UserActionProto actionProto) {

        return UserActionAvro.newBuilder()
                .setUserId(actionProto.getUserId())
                .setEventId(actionProto.getEventId())
                .setTimestamp(TimestampMapper.mapToInstant(actionProto.getTimestamp()))
                .setActionType(ActionTypeAvro.valueOf(actionProto.getActionType().name().substring(7))) //remove ACTION_
                .build();
    }
}
