package ru.practicum.feign;

import feign.FeignException;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventDto;
import ru.practicum.exceptions.NotFoundException;

import java.util.List;

@Component
public class EventClientFallback implements EventClient {
    @Override
    public Boolean categoryHasEvents(Long id) throws FeignException {
        return true;
    }

    @Override
    public EventDto findByIdAndInitiatorId(Long eventId, Long userId) throws FeignException {
        throw new NotFoundException("Event not found fallback");
    }

    @Override
    public Boolean existByIdAndInitiatorId(Long eventId, Long userId) throws FeignException {
        return false;
    }

    @Override
    public EventDto findById(Long id) throws FeignException {
        throw new NotFoundException("Event not found fallback");
    }

    @Override
    public List<EventDto> findAllByInitiatorId(Long userId) throws FeignException {
        return List.of();
    }
}
