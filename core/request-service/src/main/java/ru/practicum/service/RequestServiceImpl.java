package ru.practicum.service;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.event.EventDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.exceptions.InitiatorRequestException;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.exceptions.NotPublishEventException;
import ru.practicum.exceptions.OperationUnnecessaryException;
import ru.practicum.exceptions.ParticipantLimitException;
import ru.practicum.exceptions.RepeatableUserRequestException;
import ru.practicum.exceptions.ValidationException;
import ru.practicum.feign.EventClient;
import ru.practicum.feign.UserClient;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.Request;
import ru.practicum.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserClient usersClient;
    private final RequestMapper requestMapper;
    private final EventClient eventClient;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId, HttpServletRequest request) {
        findUserById(userId);
        return requestRepository.findByRequesterId(userId).stream()
                .map(requestMapper::requestToParticipationRequestDto)
                .toList();
    }

    private EventDto findByIdAndInitiator(Long eventId, Long userId) {
        try {
            return eventClient.findByIdAndInitiatorId(eventId, userId);
        } catch (FeignException e) {
            throw new NotFoundException(String.format("Event with id %s not found " +
                    "or unavailable for user with id %s", eventId, userId));
        }
    }

    private Boolean existByIdAndInitiator(Long eventId, Long userId) {
        try {
            return eventClient.existByIdAndInitiatorId(eventId, userId);
        } catch (FeignException e) {
            throw new NotFoundException(String.format("Event with id %s not found " +
                    "or unavailable for user with id %s", eventId, userId));
        }
    }

    private EventDto findById(Long eventId) {
        try {
            return eventClient.findById(eventId);
        } catch (FeignException e) {
            throw new NotFoundException(String.format("Event with id %s not found ", eventId));
        }
    }

    private List<EventDto> findAllByInitiatorId(Long userId) {
        try {
            return eventClient.findAllByInitiatorId(userId);
        } catch (FeignException e) {
            throw new NotFoundException(String.format("Events with user id %s not found ", userId));
        }
    }

    @Override
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        if (existByIdAndInitiator(eventId, userId)) {
            throw new InitiatorRequestException(String.format("User with id %s is initiator for event with id %s",
                    userId, eventId));
        }
        if (!requestRepository.findByRequesterIdAndEventId(userId, eventId).isEmpty()) {
            throw new RepeatableUserRequestException(String.format("User with id %s already make request for event with id %s",
                    userId, eventId));
        }
        EventDto event = findById(eventId);
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotPublishEventException(String.format("Event with id %s is not published", eventId));
        }
        Request request = new Request();
        request.setRequesterId(userId);
        request.setEventId(eventId);

        Long confirmedRequestsAmount = requestRepository.countRequestsByEventAndStatus(event.getId(), RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() <= confirmedRequestsAmount && event.getParticipantLimit() != 0) {
            throw new ParticipantLimitException(String.format("Participant limit for event with id %s id exceeded", eventId));
        }

        if (event.getParticipantLimit() == 0) {
            request.setStatus(RequestStatus.CONFIRMED);
            request.setCreatedOn(LocalDateTime.now());
            return requestMapper.requestToParticipationRequestDto(requestRepository.save(request));
        }

        if (event.isRequestModeration()) {
            request.setStatus(RequestStatus.PENDING);
            request.setCreatedOn(LocalDateTime.now());
            return requestMapper.requestToParticipationRequestDto(requestRepository.save(request));
        } else {
            request.setStatus(RequestStatus.CONFIRMED);
            request.setCreatedOn(LocalDateTime.now());
        }
        return requestMapper.requestToParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request cancellingRequest = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Request with id %s not found or unavailable " +
                        "for user with id %s", requestId, userId)));
        cancellingRequest.setStatus(RequestStatus.CANCELED);
        cancellingRequest = requestRepository.save(cancellingRequest);
        return requestMapper.requestToParticipationRequestDto(cancellingRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId, HttpServletRequest request) {
        List<EventDto> userEvents = findAllByInitiatorId(userId);
        EventDto event = userEvents.stream().filter(e -> e.getInitiator().getId().equals(userId)).findFirst()
                .orElseThrow(() -> new ValidationException(String.format("User with id %s is not initiator of event with id %s",
                        userId, eventId)));
        return requestRepository.findByEventId(event.getId())
                .stream()
                .map(requestMapper::requestToParticipationRequestDto)
                .toList();
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest eventStatusUpdate,
                                                              HttpServletRequest request) {
        EventDto event = findByIdAndInitiator(eventId, userId);
        int participantLimit = event.getParticipantLimit();
        if (participantLimit == 0 || !event.isRequestModeration()) {
            throw new OperationUnnecessaryException(String.format("Requests confirm for event with id %s is not required",
                    eventId));
        }

        List<Long> requestIds = eventStatusUpdate.getRequestIds();
        List<Request> requests = requestIds.stream()
                .map(r -> requestRepository.findByIdAndEventId(r, eventId)
                        .orElseThrow(() -> new ValidationException(String.format("Request with id %s is not apply " +
                                "to user with id %s or event with id %s", r, userId, eventId))))
                .toList();

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        long confirmedRequestsAmount;
        confirmedRequestsAmount = requestRepository.countRequestsByEventAndStatus(event.getId(), RequestStatus.CONFIRMED);
        if (confirmedRequestsAmount >= participantLimit) {
            throw new ParticipantLimitException(String.format("Participant limit for event with id %s id exceeded", eventId));
        }
        for (Request currentRequest : requests) {
            if (currentRequest.getStatus().equals(RequestStatus.PENDING)) {
                if (eventStatusUpdate.getStatus().equals(RequestStatus.CONFIRMED)) {
                    if (confirmedRequestsAmount <= participantLimit) {
                        currentRequest.setStatus(RequestStatus.CONFIRMED);
                        ParticipationRequestDto confirmed = requestMapper
                                .requestToParticipationRequestDto(currentRequest);
                        confirmedRequests.add(confirmed);
                    } else {
                        currentRequest.setStatus(RequestStatus.REJECTED);
                        ParticipationRequestDto rejected = requestMapper
                                .requestToParticipationRequestDto(currentRequest);
                        rejectedRequests.add(rejected);
                    }
                } else {
                    currentRequest.setStatus(eventStatusUpdate.getStatus());
                    ParticipationRequestDto rejected = requestMapper
                            .requestToParticipationRequestDto(currentRequest);
                    rejectedRequests.add(rejected);
                }
            }
        }
        requestRepository.saveAll(requests);
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmedRequests);
        result.setRejectedRequests(rejectedRequests);
        return result;
    }

    @Override
    public Long countRequestsByEventAndStatus(Long eId, RequestStatus status) {
        return requestRepository.countRequestsByEventAndStatus(eId, status);
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventIdInAndStatus(List<Long> ids, RequestStatus status) {
        return requestRepository.findAllByEventIdInAndStatus(ids, status)
                .stream().map(requestMapper::requestToParticipationRequestDto).toList();
    }

    @Override
    public Boolean findByRequesterIdAndEventIdAndStatus(Long userId, Long eventId, RequestStatus status) {
        return requestRepository.findByRequesterIdAndEventIdAndStatus(userId, eventId, status).isEmpty();
    }

    private void findUserById(Long userId) {
        try {
            usersClient.getUserById(userId);
        } catch (FeignException ex) {
            throw new NotFoundException(String.format("User with id %s not found", userId));
        }
    }

}
