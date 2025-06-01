package ru.practicum.events.dto;

import lombok.Data;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.user.UserDto;

@Data
public class EventShortDto {
    private Long id;
    private String title;
    private String annotation;

    private Long confirmedRequests;
    private double rating;

    private boolean paid;

    private CategoryDto category;

    private UserDto initiator;

    private String eventDate;
}