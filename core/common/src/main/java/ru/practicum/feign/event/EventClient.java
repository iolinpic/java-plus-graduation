package ru.practicum.feign.event;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="main-service",path = "/api/v1/event")
public interface EventClient {
    @GetMapping("/category")
    Boolean categoryHasEvents(@RequestParam Long id) throws FeignException;
}
