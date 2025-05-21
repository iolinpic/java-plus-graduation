package ru.practicum.events.feign;

import feign.FeignException;
import org.springframework.stereotype.Component;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;

import java.util.List;

@Component
public class RequestClientFallback implements RequestClient{
    @Override
    public Long countRequestsByEventAndStatus(Long eId, RequestStatus status) throws FeignException {
        return 0L;
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventIdInAndStatus(List<Long> ids, RequestStatus status) throws FeignException {
        return List.of();
    }

    @Override
    public Boolean findByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, RequestStatus status) throws FeignException {
        return false;
    }
}
