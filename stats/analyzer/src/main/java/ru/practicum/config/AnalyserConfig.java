package ru.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Getter
@Setter
@Configuration
@ConfigurationProperties("analyzer")
public class AnalyserConfig {
    private List<String> similarityTopics;
    private List<String> userTopics;
    private Duration consumeAttemptTimeout;
    private Properties userConsumerProperties;
    private Properties similarityConsumerProperties;


    @Bean
    public KafkaConsumer<String, UserActionAvro> userConsumer() {
        return new KafkaConsumer<>(getUserConsumerProperties());
    }

    @Bean
    public KafkaConsumer<String, EventSimilarityAvro> similarityConsumer() {
        return new KafkaConsumer<>(getSimilarityConsumerProperties());
    }
}
