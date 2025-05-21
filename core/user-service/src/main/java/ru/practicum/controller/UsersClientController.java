package ru.practicum.controller;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.user.UserDto;
import ru.practicum.feign.UserClientOperations;
import ru.practicum.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UsersClientController implements UserClientOperations {
    private final UserService userService;

    @Override
    public UserDto getUserById(Long id) throws FeignException {
        return userService.getUser(id);
    }

    @Override
    public List<UserDto> getUsersWithIds(List<Long> ids) throws FeignException {
        return userService.getUsersWithIds(ids);
    }
}
