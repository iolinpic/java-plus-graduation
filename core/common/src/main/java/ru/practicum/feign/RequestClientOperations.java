package ru.practicum.feign;

import feign.FeignException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;

import java.util.List;

public interface RequestClientOperations {

    @GetMapping("/count")
    Long countRequestsByEventAndStatus(@RequestParam Long eId, @RequestParam RequestStatus status) throws FeignException;

    @GetMapping("/list")
    List<ParticipationRequestDto> findAllByEventIdInAndStatus(@RequestParam List<Long> ids, @RequestParam RequestStatus status) throws FeignException;

    @GetMapping("/exist")
    Boolean findByRequesterIdAndEventIdAndStatus(@RequestParam Long userId, @RequestParam Long eventId, @RequestParam RequestStatus status) throws FeignException;
}
