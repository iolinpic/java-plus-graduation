package ru.practicum.events.controller;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.events.service.EventService;
import ru.practicum.feign.event.EventClient;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/event")
public class EventClientController implements EventClient {
    private final EventService eventService;

    @Override
    public Boolean categoryHasEvents(Long id) throws FeignException {
        return eventService.checkIfCategoryHasEvents(id);
    }
}
