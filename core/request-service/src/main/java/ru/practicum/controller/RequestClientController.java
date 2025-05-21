package ru.practicum.controller;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.feign.request.RequestClient;
import ru.practicum.service.RequestService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/request")
public class RequestClientController implements RequestClient {
    private final RequestService requestService;

    @Override
    public Long countRequestsByEventAndStatus(Long eId, RequestStatus status) {
        return requestService.countRequestsByEventAndStatus(eId,status);
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventIdInAndStatus(List<Long> ids, RequestStatus status) {
        return requestService.findAllByEventIdInAndStatus(ids, status);
    }

    @Override
    public Boolean findByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, RequestStatus status) throws FeignException {
        return null;
    }
}
