package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId, HttpServletRequest request);

    ParticipationRequestDto addParticipationRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId, HttpServletRequest request);

    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                       EventRequestStatusUpdateRequest eventStatusUpdate,
                                                       HttpServletRequest request);

    Long countRequestsByEventAndStatus(Long eId, RequestStatus status);

    List<ParticipationRequestDto> findAllByEventIdInAndStatus(List<Long> ids, RequestStatus status);

    Boolean findByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, RequestStatus status);
}
