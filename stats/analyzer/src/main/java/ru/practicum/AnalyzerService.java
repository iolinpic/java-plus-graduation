package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyzerService {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerService.class, args);

        KafkaConsumerService kafkaConsumer = context.getBean(KafkaConsumerService.class);
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaConsumer::stop));
        kafkaConsumer.start();
    }
}