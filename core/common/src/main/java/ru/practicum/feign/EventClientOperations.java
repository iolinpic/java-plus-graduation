package ru.practicum.feign;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.event.EventDto;

import java.util.List;


public interface EventClientOperations {
    @GetMapping("/category")
    Boolean categoryHasEvents(@RequestParam Long id) throws FeignException;

    @GetMapping("/initiation")
    EventDto findByIdAndInitiatorId(@RequestParam Long eventId,@RequestParam Long userId) throws FeignException;

    @GetMapping("/initiation/exist")
    Boolean existByIdAndInitiatorId(@RequestParam Long eventId,@RequestParam Long userId) throws FeignException;

    @GetMapping("/{id}")
    EventDto findById(@PathVariable Long id) throws FeignException;

    @GetMapping("/user")
    List<EventDto> findAllByInitiatorId(@RequestParam Long userId) throws FeignException;
}
