package ru.practicum.events.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.feign.CategoryClientOperations;

@FeignClient(name = "category-service", path = "/api/v1/category",
        fallback = CategoryClientFallback.class)
public interface CategoryClient extends CategoryClientOperations {
}
