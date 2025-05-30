package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AggregatorService {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AggregatorService.class, args);
        AggregationStarter aggregator = context.getBean(AggregationStarter.class);
        Runtime.getRuntime().addShutdownHook(new Thread(aggregator::stop));
        aggregator.start();
    }
}