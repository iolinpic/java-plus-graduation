package ru.practicum.events.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.feign.UserClientOperations;

@FeignClient(name = "user-service", path = "/api/v1/user",
        fallback = UserClientFallback.class)
public interface UserClient extends UserClientOperations {
}
