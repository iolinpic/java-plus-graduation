package ru.practicum.client;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;
import ru.practicum.exception.ClientException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class StatClient {

    private final DiscoveryClient discoveryClient;
    private RetryTemplate retryTemplate;


    private final RestClient restClient;

    @Autowired
    public StatClient(@Autowired DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        this.restClient = RestClient.create();
        this.retryTemplate = new RetryTemplate();
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
    }


    private ServiceInstance getInstance() {
        String serviceId = "stat-server";
        try {
            return discoveryClient
                    .getInstances(serviceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + serviceId,
                    exception
            );
        }
    }
    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(cxt -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }



    public void saveHit(EndpointHitDto hitDto) {
        String uri = UriComponentsBuilder.newInstance()
                .uri(makeUri("/hit"))
                .toUriString();

        restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .body(hitDto)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ClientException(
                            response.getStatusCode().value(),
                            response.getBody().toString()
                    );
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ClientException(
                            response.getStatusCode().value(),
                            response.getBody().toString()
                    );
                })
                .toBodilessEntity();
    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String uriWithParams = UriComponentsBuilder.newInstance()
                .uri(makeUri("/stats"))
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .toUriString();

        return restClient.get()
                .uri(uriWithParams).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new ClientException(
                            response.getStatusCode().value(),
                            response.getBody().toString()
                    );
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new ClientException(
                            response.getStatusCode().value(),
                            response.getBody().toString()
                    );
                })
                .body(new ParameterizedTypeReference<>() {
                });
    }

}
