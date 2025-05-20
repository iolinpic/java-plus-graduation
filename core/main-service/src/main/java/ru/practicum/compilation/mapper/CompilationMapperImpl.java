package ru.practicum.compilation.mapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.exceptions.NotFoundException;
import ru.practicum.feign.category.CategoryClient;
import ru.practicum.feign.users.UsersClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Component
public final class CompilationMapperImpl {
    private final EventMapper eventMapper;
    private final UsersClient usersClient;
    private final CategoryClient categoryClient;

    public CompilationDto toDto(Compilation compilation) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setPinned(compilation.isPinned());
        dto.setTitle(compilation.getTitle());
        dto.setEvents(mapShortAndAddUsersAndCategories(compilation.getEvents()));
        return dto;
    }

    private Map<Long, UserDto> loadUsers(List<Long> ids) {
        try {
            return usersClient.getUsersWithIds(ids).stream().collect(Collectors.toMap(UserDto::getId, user -> user));
        } catch (FeignException e) {
            throw new NotFoundException("Some users load error");
        }
    }

    private List<EventShortDto> mapShortAndAddUsersAndCategories(Set<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).toList();
        List<Long> categoryIds = events.stream().map(Event::getInitiatorId).toList();
        Map<Long, UserDto> users = loadUsers(ids);
        Map<Long, CategoryDto> categories = loadCategories(categoryIds);
        return events.stream().map(e ->
                eventMapper.toEventShortDto(e, users.get(e.getInitiatorId()),categories.get(e.getCategoryId()))).toList();
    }



    private Map<Long, CategoryDto> loadCategories(List<Long> ids) {
        try {
            return categoryClient.findByIds(ids).stream()
                    .collect(Collectors.toMap(CategoryDto::getId, cat -> cat));
        } catch (FeignException e) {
            throw new NotFoundException("Some users load error");
        }
    }

}
