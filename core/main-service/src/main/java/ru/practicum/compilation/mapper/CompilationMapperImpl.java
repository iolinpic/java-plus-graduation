package ru.practicum.compilation.mapper;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.dto.user.UserDto;
import ru.practicum.events.dto.EventShortDto;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.events.model.Event;
import ru.practicum.exceptions.NotFoundException;
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

    public CompilationDto toDto(Compilation compilation) {
        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setPinned(compilation.isPinned());
        dto.setTitle(compilation.getTitle());
        dto.setEvents(mapShortAndAddUsers(compilation.getEvents()));
        return dto;
    }

    private Map<Long, UserDto> loadUsers(List<Long> ids) {
        try {
            return usersClient.getUsersWithIds(ids).stream().collect(Collectors.toMap(UserDto::getId, user -> user));
        } catch (FeignException e) {
            throw new NotFoundException("Some users load error");
        }
    }

    private List<EventShortDto> mapShortAndAddUsers(Set<Event> events) {
        List<Long> ids = events.stream().map(Event::getInitiatorId).toList();
        Map<Long, UserDto> users = loadUsers(ids);
        return events.stream().map(e -> eventMapper.toEventShortDto(e, users.get(e.getInitiatorId()))).toList();
    }

}
