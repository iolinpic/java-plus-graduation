package ru.practicum.events.service;

import com.querydsl.core.types.Predicate;
import feign.FeignException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.client.StatClient;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.comment.enums.CommentStatus;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.dto.ViewStats;
import ru.practicum.dto.user.UserDto;
import ru.practicum.events.dto.AdminUpdateStateAction;
import ru.practicum.events.dto.EntityParam;
import ru.practicum.events.dto.EventAdminUpdateDto;
import ru.practicum.events.dto.EventCreateDto;
import ru.practicum.events.dto.EventDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.EventUpdateDto;
import ru.practicum.events.dto.LocationDto;
import ru.practicum.events.dto.SearchEventsParam;
import ru.practicum.events.dto.UpdateStateAction;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.mapper.LocationMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventSort;
import ru.practicum.events.model.EventState;
import ru.practicum.events.model.Location;
import ru.practicum.events.predicates.EventPredicates;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.repository.LocationRepository;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.exceptions.OperationForbiddenException;
import ru.practicum.feign.users.UsersClient;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;
import ru.practicum.request.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final LocationRepository locationRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final StatClient statClient;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UsersClient usersClient;

    @Override
    public List<EventDto> adminEventsSearch(SearchEventsParam param) {
        Pageable pageable = PageRequest.of(param.getFrom(), param.getSize());
        Predicate predicate = EventPredicates.adminFilter(param);
        List<Event> events;
        if (predicate == null) {
            events = eventRepository.findAll(pageable).toList();
        } else {
            events = eventRepository.findAll(predicate, pageable).toList();
        }
        return addMinimalDataToList(mapAndAddUsers(events));
    }


    @Override
    public EventDto adminEventUpdate(Long eventId, EventAdminUpdateDto eventUpdateDto) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event not found"));
        if (eventUpdateDto.getEventDate() != null && eventUpdateDto.getEventDate().isBefore(event.getCreatedOn().minusHours(1))) {
            throw new ValidationException("Event date cannot be before created date");
        }
        UserDto user = findUserById(event.getInitiatorId());

        updateEventData(event, eventUpdateDto.getTitle(),
                eventUpdateDto.getAnnotation(),
                eventUpdateDto.getDescription(),
                eventUpdateDto.getCategory(),
                eventUpdateDto.getEventDate(),
                eventUpdateDto.getLocation(),
                eventUpdateDto.getPaid(),
                eventUpdateDto.getRequestModeration(),
                eventUpdateDto.getParticipantLimit());
        if (eventUpdateDto.getStateAction() != null) {
            if (!event.getState().equals(EventState.PENDING)) {
                throw new OperationForbiddenException("Can't reject not pending event");
            }
            if (eventUpdateDto.getStateAction().equals(AdminUpdateStateAction.PUBLISH_EVENT)) {
                event.setState(EventState.PUBLISHED);
            }
            if (eventUpdateDto.getStateAction().equals(AdminUpdateStateAction.REJECT_EVENT)) {
                event.setState(EventState.CANCELED);
            }
        }
        event = eventRepository.save(event);
        return addAdvancedData(eventMapper.toDto(event, user));
    }

    @Override
    public List<EventShortDto> getEvents(EntityParam params) {
        LocalDateTime rangeStart = params.getRangeStart();
        LocalDateTime rangeEnd = params.getRangeEnd();
        if (rangeStart != null && rangeEnd != null) {
            if (rangeStart.isAfter(rangeEnd)) {
                throw new ValidationException("Start date can not be after end date");
            }
        }

        Predicate predicate = EventPredicates.publicFilter(params.getText(), params.getCategories(), rangeStart,
                rangeEnd, params.getPaid());
        Pageable pageable = PageRequest.of(params.getFrom(), params.getSize());

        List<Event> filteredEvents;
        if (predicate != null) {
            filteredEvents = eventRepository.findAll(predicate, pageable).toList();
        } else {
            filteredEvents = eventRepository.findAll(pageable).toList();
        }

        if (params.getOnlyAvailable()) {
            filteredEvents = filteredEvents.stream().filter(this::isEventAvailable).toList();
        }
        List<EventShortDto> eventDtos = addAdvancedDataToShortDtoList(mapShortAndAddUsers(filteredEvents));

        EventSort sort = params.getSort();
        if (sort != null) {
            switch (sort) {
                case EVENT_DATE ->
                        eventDtos = eventDtos.stream().sorted(Comparator.comparing(EventShortDto::getEventDate)).toList();
                case VIEWS ->
                        eventDtos = eventDtos.stream().sorted(Comparator.comparing(EventShortDto::getViews)).toList();
            }
        }
        return eventDtos;
    }

    @Override
    public EventDto getEvent(Long eventId) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", eventId)));
        UserDto user = findUserById(event.getInitiatorId());
        return addAdvancedData(eventMapper.toDto(event, user));
    }


    private void updateEventData(Event event, String title, String annotation, String description, Long categoryId, LocalDateTime eventDate, LocationDto location, Boolean paid, Boolean requestModeration, Integer participantLimit) {
        if (title != null) {
            event.setTitle(title);
        }
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new NotFoundException("Category not found"));
            event.setCategory(category);
        }
        if (eventDate != null) {
            if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
                throw new ValidationException(String.format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s", eventDate));
            }
            event.setEventDate(eventDate);
        }
        if (location != null) {
            Location newLocation = locationRepository.save(locationMapper.toLocation(location));
            event.setLocation(newLocation);
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
    }

    @Override
    public List<EventDto> privateUserEvents(Long userId, int from, int size) {
        Pageable pageable = PageRequest.of(from, size);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);
        return addAdvancedDataToList(mapAndAddUsers(events));
    }

    @Override
    public EventDto privateEventCreate(Long userId, EventCreateDto eventCreateDto) {
        if (eventCreateDto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException(String
                    .format("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: %s",
                            eventCreateDto.getEventDate()));
        }
        UserDto user = findUserById(userId);
        Event event = eventMapper.fromDto(eventCreateDto);
        event.setInitiatorId(userId);
        Category category = categoryRepository.findById(eventCreateDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Category not found"));
        event.setCategory(category);
        locationRepository.save(event.getLocation());
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        event = eventRepository.save(event);
        return addAdvancedData(eventMapper.toDto(event, user));
    }

    @Override
    public EventDto privateGetUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", eventId)));
        UserDto user = findUserById(event.getInitiatorId());
        return addAdvancedData(eventMapper.toDto(event, user));
    }

    @Override
    public EventDto privateUpdateUserEvent(Long userId, Long eventId, EventUpdateDto eventUpdateDto) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", eventId)));
        if (event.getState().equals(EventState.PUBLISHED) || event.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new OperationForbiddenException("Only pending or canceled events can be changed");
        }
        updateEventData(event, eventUpdateDto.getTitle(),
                eventUpdateDto.getAnnotation(),
                eventUpdateDto.getDescription(),
                eventUpdateDto.getCategory(),
                eventUpdateDto.getEventDate(),
                eventUpdateDto.getLocation(),
                eventUpdateDto.getPaid(),
                eventUpdateDto.getRequestModeration(),
                eventUpdateDto.getParticipantLimit());
        if (eventUpdateDto.getStateAction() != null) {
            if (eventUpdateDto.getStateAction().equals(UpdateStateAction.SEND_TO_REVIEW)) {
                event.setState(EventState.PENDING);
            }
            if (eventUpdateDto.getStateAction().equals(UpdateStateAction.CANCEL_REVIEW)) {
                event.setState(EventState.CANCELED);
            }
        }
        event = eventRepository.save(event);
        UserDto user = findUserById(event.getInitiatorId());
        return addAdvancedData(eventMapper.toDto(event, user));
    }

    private EventDto addAdvancedData(EventDto eventDto) {
        List<String> gettingUris = new ArrayList<>();
        gettingUris.add("/events/" + eventDto.getId());
        Long views = statClient.getStats(LocalDateTime.now().minusYears(1), LocalDateTime.now(), gettingUris, true)
                .stream().map(ViewStats::getHits).reduce(0L, Long::sum);
        eventDto.setViews(views);

        eventDto.setConfirmedRequests(requestRepository.countRequestsByEventAndStatus(eventRepository.findById(
                eventDto.getId()).get(), RequestStatus.CONFIRMED));

        List<CommentDto> comments = commentRepository.findByEventIdAndStatus(eventDto.getId(), CommentStatus.PUBLISHED)
                .stream()
                .map(commentMapper::toDto)
                .toList();
        eventDto.setComments(comments);

        return eventDto;
    }

    private boolean isEventAvailable(Event event) {
        Long confirmedRequestsAmount = requestRepository.countRequestsByEventAndStatus(event, RequestStatus.CONFIRMED);
        return event.getParticipantLimit() > confirmedRequestsAmount;
    }

    private HashMap<Long, Long> getEventConfirmedRequestsCount(List<Long> idsList) {
        List<Request> requests = requestRepository.findAllByEventIdInAndStatus(idsList, RequestStatus.CONFIRMED);
        HashMap<Long, Long> confirmedRequestMap = new HashMap<>();
        for (Request request : requests) {
            if (confirmedRequestMap.containsKey(request.getEvent().getId())) {
                confirmedRequestMap.put(request.getEvent().getId(), confirmedRequestMap.get(request.getEvent().getId()) + 1);
            } else {
                confirmedRequestMap.put(request.getEvent().getId(), 1L);
            }
        }
        for (Long id : idsList) {
            if (!confirmedRequestMap.containsKey(id)) {
                confirmedRequestMap.put(id, 0L);
            }
        }
        return confirmedRequestMap;
    }

    private HashMap<Long, List<CommentDto>> getEventComments(List<Long> idsList) {
        List<CommentDto> comments = commentRepository.findAllByEventIdInAndStatus(idsList, CommentStatus.PUBLISHED)
                .stream()
                .map(commentMapper::toDto)
                .toList();
        HashMap<Long, List<CommentDto>> commentsMap = new HashMap<>();
        for (CommentDto comment : comments) {
            if (!commentsMap.containsKey(comment.getEventId())) {
                commentsMap.put(comment.getEventId(), new ArrayList<>());
            }
            commentsMap.get(comment.getEventId()).add(comment);
        }
        return commentsMap;
    }

    private HashMap<Long, Long> getEventViews(List<Long> idsList) {
        List<String> uris = idsList.stream().map(id -> "/events/" + id).toList();
        List<ViewStats> viewStats = statClient.getStats(LocalDateTime.now().minusYears(1), LocalDateTime.now(), uris, false);
        HashMap<Long, Long> viewMap = new HashMap<>();
        for (Long id : idsList) {
            viewMap.put(id, viewStats.stream().filter(v -> v.getUri().equals("/events/" + id)).map(ViewStats::getHits).findFirst().orElse(0L));
        }
        return viewMap;
    }

    private List<EventDto> addMinimalDataToList(List<EventDto> eventDtoList) {

        List<Long> idsList = eventDtoList.stream().map(EventDto::getId).toList();
        HashMap<Long, Long> confirmedMap = getEventConfirmedRequestsCount(idsList);

        return eventDtoList.stream()
                .peek(dto -> dto.setConfirmedRequests(confirmedMap.get(dto.getId())))
                .toList();
    }

    private List<EventDto> addAdvancedDataToList(List<EventDto> eventDtoList) {

        List<Long> idsList = eventDtoList.stream().map(EventDto::getId).toList();
        HashMap<Long, Long> viewsMap = getEventViews(idsList);
        HashMap<Long, Long> confirmedMap = getEventConfirmedRequestsCount(idsList);
        HashMap<Long, List<CommentDto>> commentMap = getEventComments(idsList);

        return eventDtoList.stream()
                .peek(dto -> dto.setComments(commentMap.get(dto.getId())))
                .peek(dto -> dto.setViews(viewsMap.get(dto.getId())))
                .peek(dto -> dto.setConfirmedRequests(confirmedMap.get(dto.getId())))
                .toList();
    }

    private List<EventShortDto> addAdvancedDataToShortDtoList(List<EventShortDto> eventShortDtoList) {

        List<Long> idsList = eventShortDtoList.stream().map(EventShortDto::getId).toList();
        HashMap<Long, Long> viewsMap = getEventViews(idsList);
        HashMap<Long, Long> confirmedMap = getEventConfirmedRequestsCount(idsList);

        return eventShortDtoList.stream()
                .peek(dto -> dto.setViews(viewsMap.get(dto.getId())))
                .peek(dto -> dto.setConfirmedRequests(confirmedMap.get(dto.getId())))
                .toList();
    }

    private UserDto findUserById(Long userId) {
        try {
            return usersClient.getUserById(userId);
        } catch (FeignException ex) {
            throw new NotFoundException(String.format("User with id %s not found", userId));
        }
    }

    private Map<Long, UserDto> loadUsers(List<Long> ids) {
        try {
            return usersClient.getUsersWithIds(ids).stream().collect(Collectors.toMap(UserDto::getId, user -> user));
        } catch (FeignException e) {
            throw new NotFoundException("Some users load error");
        }
    }

    private List<EventDto> mapAndAddUsers(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).toList();
        Map<Long, UserDto> users = loadUsers(ids);
        return events.stream().map(e -> eventMapper.toDto(e, users.get(e.getInitiatorId()))).toList();
    }

    private List<EventShortDto> mapShortAndAddUsers(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).toList();
        Map<Long, UserDto> users = loadUsers(ids);
        return events.stream().map(e -> eventMapper.toEventShortDto(e, users.get(e.getInitiatorId()))).toList();
    }
}
