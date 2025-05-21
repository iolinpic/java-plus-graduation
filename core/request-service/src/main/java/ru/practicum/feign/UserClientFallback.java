package ru.practicum.feign;

import feign.FeignException;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@Component
public class UserClientFallback implements UserClient {
    @Override
    public UserDto getUserById(Long id) throws FeignException {
        return createDtoWithId(id);
    }

    @Override
    public List<UserDto> getUsersWithIds(List<Long> ids) throws FeignException {
        return ids.stream().map(this::createDtoWithId).toList();
    }

    private UserDto createDtoWithId(Long id) {
        UserDto dto = new UserDto();
        dto.setId(id);
        return dto;
    }
}
