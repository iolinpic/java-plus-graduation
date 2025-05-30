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

        SimilarityConsumer similarityConsumer = context.getBean(SimilarityConsumer.class);
        Runtime.getRuntime().addShutdownHook(new Thread(similarityConsumer::stop));
        similarityConsumer.start();

        UserActionConsumer userActionConsumer = context.getBean(UserActionConsumer.class);
        Runtime.getRuntime().addShutdownHook(new Thread(userActionConsumer::stop));
        userActionConsumer.start();
    }
}