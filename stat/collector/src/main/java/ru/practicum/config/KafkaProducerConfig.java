package ru.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Getter
@Setter
@Configuration
@ConfigurationProperties("collector")
public class KafkaProducerConfig {
    private String topic;
    private Properties producerProperties;

    @Bean
    public KafkaProducer<String, SpecificRecordBase> producer() {
        return new KafkaProducer<>(getProducerProperties());
    }

}
