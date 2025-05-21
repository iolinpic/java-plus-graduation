package ru.practicum.category.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.feign.EventClientOperations;

@FeignClient(name = "event-service", path = "/api/v1/event",
        fallback = EventClientFallback.class)
public interface EventClient extends EventClientOperations {
}
