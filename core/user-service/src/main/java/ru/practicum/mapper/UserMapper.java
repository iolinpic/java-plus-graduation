package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.dto.user.UserDto;
import ru.practicum.model.User;


@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toUserDto(User user);

    User toUser(UserDto userDto);
}
