package ru.practicum.service;


import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto createUser(UserDto userDto);

    void deleteUser(Long userId);

    UserDto getUser(Long userId);
    List<UserDto> getUsersWithIds(List<Long> ids);
}
