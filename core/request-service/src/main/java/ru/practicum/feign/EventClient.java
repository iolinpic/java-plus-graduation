package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "main-service", path = "/api/v1/event",
        fallback = EventClientFallback.class)
public interface EventClient extends EventClientOperations {
}
