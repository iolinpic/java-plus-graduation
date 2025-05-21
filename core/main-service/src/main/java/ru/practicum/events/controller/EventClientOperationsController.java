package ru.practicum.events.controller;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.event.EventDto;
import ru.practicum.events.service.EventService;
import ru.practicum.feign.EventClientOperations;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/event")
public class EventClientOperationsController implements EventClientOperations {
    private final EventService eventService;

    @Override
    public Boolean categoryHasEvents(Long id) throws FeignException {
        return eventService.checkIfCategoryHasEvents(id);
    }

    @Override
    public EventDto findByIdAndInitiatorId(Long eventId, Long userId) throws FeignException {
        return eventService.findByIdAndInitiatorId(eventId,userId);
    }

    @Override
    public Boolean existByIdAndInitiatorId(Long eventId, Long userId) throws FeignException {
        return eventService.existByIdAndInitiatorId(eventId,userId);
    }

    @Override
    public EventDto findById(Long id) throws FeignException {
        return eventService.findById(id);
    }

    @Override
    public List<EventDto> findAllByInitiatorId(Long userId) throws FeignException {
        return eventService.findAllByInitiatorId(userId);
    }
}
