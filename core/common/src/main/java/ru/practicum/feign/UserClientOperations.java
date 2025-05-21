package ru.practicum.feign;

import feign.FeignException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDto;

import java.util.List;

public interface UserClientOperations {
    @GetMapping
    UserDto getUserById(@RequestParam Long id) throws FeignException;

    @GetMapping("/list")
    List<UserDto> getUsersWithIds(@RequestParam List<Long> ids) throws FeignException;

    ;
}
