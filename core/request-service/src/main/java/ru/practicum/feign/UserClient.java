package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "user-service", path = "/api/v1/user",
        fallback = UserClientFallback.class)
public interface UserClient extends UserClientOperations {
}
