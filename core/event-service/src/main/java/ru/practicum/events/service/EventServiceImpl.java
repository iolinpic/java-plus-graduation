package ru.practicum.events.service;

import com.querydsl.core.types.Predicate;
import feign.FeignException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.client.AnalyzerClient;
import ru.practicum.comment.enums.CommentStatus;
import ru.practicum.comment.mapper.CommentMapper;
import ru.practicum.comment.repository.CommentRepository;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.event.EventDto;
import ru.practicum.dto.event.EventState;
import ru.practicum.dto.event.LocationDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.dto.user.UserDto;
import ru.practicum.events.dto.AdminUpdateStateAction;
import ru.practicum.events.dto.EntityParam;
import ru.practicum.events.dto.EventAdminUpdateDto;
import ru.practicum.events.dto.EventCreateDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.dto.EventUpdateDto;
import ru.practicum.events.dto.SearchEventsParam;
import ru.practicum.events.dto.UpdateStateAction;
import ru.practicum.events.feign.CategoryClient;
import ru.practicum.events.feign.RequestClient;
import ru.practicum.events.feign.UserClient;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.mapper.LocationMapper;
import ru.practicum.events.model.Event;
import ru.practicum.events.model.EventSort;
import ru.practicum.events.model.Location;
import ru.practicum.events.predicates.EventPredicates;
import ru.practicum.events.repository.EventRepository;
import ru.practicum.events.repository.LocationRepository;
import ru.practicum.ewm.stats.grpc.predict.RecommendedEventProto;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.exceptions.OperationForbiddenException;

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
    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserClient userClient;
    private final CategoryClient categoryClient;
    private final RequestClient requestClient;
    private final AnalyzerClient analyzerClient;

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
        return addMinimalDataToList(mapAndAddUsersAndCategories(events));
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
        CategoryDto categoryDto = findCategoryById(event.getCategoryId());
        return addAdvancedData(eventMapper.toDto(event, user, categoryDto));
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
        List<EventShortDto> eventDtos = addAdvancedDataToShortDtoList(mapShortAndAddUsersAndCategories(filteredEvents));

        EventSort sort = params.getSort();
        if (sort != null) {
            switch (sort) {
                case EVENT_DATE ->
                        eventDtos = eventDtos.stream().sorted(Comparator.comparing(EventShortDto::getEventDate)).toList();
                case VIEWS ->
                        eventDtos = eventDtos.stream().sorted(Comparator.comparing(EventShortDto::getRating)).toList();
            }
        }
        return eventDtos;
    }

    @Override
    public EventDto getEvent(Long eventId) {
        Event event = eventRepository.findByIdAndState(eventId, EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", eventId)));
        UserDto user = findUserById(event.getInitiatorId());
        CategoryDto categoryDto = findCategoryById(event.getCategoryId());
        return addAdvancedData(eventMapper.toDto(event, user, categoryDto));
    }

    @Override
    public Boolean checkIfCategoryHasEvents(Long catId) {
        return !eventRepository.findAllByCategoryId(catId).isEmpty();
    }

    @Override
    public EventDto findByIdAndInitiatorId(Long eventId, Long userId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("event not found"));
        UserDto user = userClient.getUserById(event.getInitiatorId());
        CategoryDto category = findCategoryById(event.getCategoryId());
        return addAdvancedData(eventMapper.toDto(event, user, category));
    }

    @Override
    public Boolean existByIdAndInitiatorId(Long eventId, Long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId).isPresent();
    }

    @Override
    public EventDto findById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("event not found"));
        UserDto user = userClient.getUserById(event.getInitiatorId());
        CategoryDto category = findCategoryById(event.getCategoryId());
        return addAdvancedData(eventMapper.toDto(event, user, category));
    }

    @Override
    public List<EventDto> findAllByInitiatorId(Long userId) {
        List<Event> events = eventRepository.findAllByInitiatorId(userId);
        return addMinimalDataToList(mapAndAddUsersAndCategories(events));
    }

    @Override
    public void likeEvent(Long eventId, Long userId) {
        try{
            //проверяем что у юзера есть заявка
            requestClient.findByRequesterIdAndEventIdAndStatus(userId,eventId,RequestStatus.CONFIRMED);
        } catch (FeignException e) {
            throw new ru.practicum.exceptions.ValidationException("Пользователь может лайкать только посещённые им мероприятия");
        }

    }

    @Override
    public List<EventDto> getRecommendations(Long userId, int maxResults) {
        List<Long> ids = analyzerClient.getRecommendations(userId, maxResults).stream()
                .sorted((a, b) -> (int) (a.getScore() - b.getScore()))
                .map(RecommendedEventProto::getEventId).toList();
        List<Event> events = eventRepository.findAllById(ids);
        return addAdvancedDataToList(mapAndAddUsersAndCategories(events));
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
            findCategoryById(categoryId);
            event.setCategoryId(categoryId);
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
        return addAdvancedDataToList(mapAndAddUsersAndCategories(events));
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
        locationRepository.save(event.getLocation());
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        event = eventRepository.save(event);
        CategoryDto categoryDto = findCategoryById(event.getCategoryId());
        return addAdvancedData(eventMapper.toDto(event, user, categoryDto));
    }

    @Override
    public EventDto privateGetUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(String.format("Event with id %s not found", eventId)));
        UserDto user = findUserById(event.getInitiatorId());
        CategoryDto categoryDto = findCategoryById(event.getCategoryId());
        return addAdvancedData(eventMapper.toDto(event, user, categoryDto));
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
        CategoryDto categoryDto = findCategoryById(event.getCategoryId());
        return addAdvancedData(eventMapper.toDto(event, user, categoryDto));
    }

    private EventDto addAdvancedData(EventDto eventDto) {
        Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(List.of(eventDto.getId()));
        eventDto.setRating(ratingMap.get(eventDto.getId()));

        eventDto.setConfirmedRequests(requestClient.countRequestsByEventAndStatus(
                eventDto.getId(), RequestStatus.CONFIRMED));

        List<CommentDto> comments = commentRepository.findByEventIdAndStatus(eventDto.getId(), CommentStatus.PUBLISHED)
                .stream()
                .map(commentMapper::toDto)
                .toList();
        eventDto.setComments(comments);

        return eventDto;
    }

    private boolean isEventAvailable(Event event) {
        Long confirmedRequestsAmount = requestClient.countRequestsByEventAndStatus(event.getId(), RequestStatus.CONFIRMED);
        return event.getParticipantLimit() > confirmedRequestsAmount;
    }

    private HashMap<Long, Long> getEventConfirmedRequestsCount(List<Long> idsList) {
        List<ParticipationRequestDto> requests = requestClient.findAllByEventIdInAndStatus(idsList, RequestStatus.CONFIRMED);
        HashMap<Long, Long> confirmedRequestMap = new HashMap<>();
        for (ParticipationRequestDto request : requests) {
            if (confirmedRequestMap.containsKey(request.getEvent())) {
                confirmedRequestMap.put(request.getEvent(), confirmedRequestMap.get(request.getEvent()) + 1);
            } else {
                confirmedRequestMap.put(request.getEvent(), 1L);
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


    private List<EventDto> addMinimalDataToList(List<EventDto> eventDtoList) {

        List<Long> idsList = eventDtoList.stream().map(EventDto::getId).toList();
        HashMap<Long, Long> confirmedMap = getEventConfirmedRequestsCount(idsList);

        return eventDtoList.stream()
                .peek(dto -> dto.setConfirmedRequests(confirmedMap.get(dto.getId())))
                .toList();
    }

    private List<EventDto> addAdvancedDataToList(List<EventDto> eventDtoList) {

        List<Long> idsList = eventDtoList.stream().map(EventDto::getId).toList();
        Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(idsList);
        HashMap<Long, Long> confirmedMap = getEventConfirmedRequestsCount(idsList);
        HashMap<Long, List<CommentDto>> commentMap = getEventComments(idsList);

        return eventDtoList.stream()
                .peek(dto -> dto.setComments(commentMap.get(dto.getId())))
                .peek(dto -> dto.setRating(ratingMap.get(dto.getId())))
                .peek(dto -> dto.setConfirmedRequests(confirmedMap.get(dto.getId())))
                .toList();
    }

    private List<EventShortDto> addAdvancedDataToShortDtoList(List<EventShortDto> eventShortDtoList) {

        List<Long> idsList = eventShortDtoList.stream().map(EventShortDto::getId).toList();
        Map<Long, Double> ratingMap = analyzerClient.getInteractionsCount(idsList);
        HashMap<Long, Long> confirmedMap = getEventConfirmedRequestsCount(idsList);

        return eventShortDtoList.stream()
                .peek(dto -> dto.setRating(ratingMap.get(dto.getId())))
                .peek(dto -> dto.setConfirmedRequests(confirmedMap.get(dto.getId())))
                .toList();
    }

    private UserDto findUserById(Long userId) {
        try {
            return userClient.getUserById(userId);
        } catch (FeignException ex) {
            throw new NotFoundException(String.format("User with id %s not found", userId));
        }
    }

    private Map<Long, UserDto> loadUsers(List<Long> ids) {
        try {
            return userClient.getUsersWithIds(ids).stream().collect(Collectors.toMap(UserDto::getId, user -> user));
        } catch (FeignException e) {
            throw new NotFoundException("Some users load error");
        }
    }

    private List<EventDto> mapAndAddUsersAndCategories(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).toList();
        List<Long> categoryIds = events.stream().map(Event::getCategoryId).toList();
        Map<Long, UserDto> users = loadUsers(ids);
        Map<Long, CategoryDto> categories = loadCategories(categoryIds);
        return events.stream().map(e ->
                eventMapper.toDto(e, users.get(e.getInitiatorId()),
                        categories.get(e.getCategoryId()))).toList();
    }

    private List<EventShortDto> mapShortAndAddUsersAndCategories(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).toList();
        List<Long> categoryIds = events.stream().map(Event::getCategoryId).toList();
        Map<Long, UserDto> users = loadUsers(ids);
        Map<Long, CategoryDto> categories = loadCategories(categoryIds);
        return events.stream().map(e ->
                eventMapper.toEventShortDto(e, users.get(e.getInitiatorId()), categories.get(e.getCategoryId()))).toList();
    }

    private CategoryDto findCategoryById(Long categoryId) {
        try {
            return categoryClient.findById(categoryId);
        } catch (FeignException ex) {
            throw new NotFoundException(String.format("Category with id %s not found", categoryId));
        }
    }

    private Map<Long, CategoryDto> loadCategories(List<Long> ids) {
        try {
            return categoryClient.findByIds(ids).stream()
                    .collect(Collectors.toMap(CategoryDto::getId, cat -> cat));
        } catch (FeignException e) {
            throw new NotFoundException("Some categories load error");
        }
    }
}
