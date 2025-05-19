package ru.practicum.feign.users;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.user.UserDto;

import java.util.List;

@FeignClient(name="user-service", path = "/api/v1/user")
public interface UsersClient {
    @GetMapping
    UserDto getUserById(@RequestParam Long id) throws FeignException;;
    @GetMapping("/list")
    List<UserDto> getUsersWithIds(@RequestParam List<Long> ids) throws FeignException;;
}
