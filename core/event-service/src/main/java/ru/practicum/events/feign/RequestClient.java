package ru.practicum.events.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.feign.RequestClientOperations;

@FeignClient(name = "request-service", path = "/api/v1/request",
        fallback = RequestClientFallback.class)
public interface RequestClient extends RequestClientOperations {
}
