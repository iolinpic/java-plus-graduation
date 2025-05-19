package ru.practicum.controller;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.dto.user.UserDto;
import ru.practicum.feign.users.UsersClient;
import ru.practicum.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UsersClientController implements UsersClient {
    private final UserService userService;

    @Override
    public UserDto getUserById(Long id) throws FeignException {
        log.info("Getting user by id: {}", id);
        return userService.getUser(id);
    }

    @Override
    public List<UserDto> getUsersWithIds(List<Long> ids) throws FeignException {
        log.info("Getting users with ids: {}", ids);
        return userService.getUsersWithIds(ids);
    }
}
