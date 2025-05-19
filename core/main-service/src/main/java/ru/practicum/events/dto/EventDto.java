package ru.practicum.events.dto;

import lombok.Data;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.comment.dto.CommentDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.events.model.EventState;

import java.util.List;

@Data
public class EventDto {
    private Long id;
    private String title;
    private String annotation;
    private String description;

    private long confirmedRequests;
    private long views;

    private boolean paid;
    private boolean requestModeration;
    private int participantLimit;

    private CategoryDto category;
    private LocationDto location;

    private EventState state;
    private UserDto initiator;

    private String eventDate;
    private String createdOn;
    private String publishedOn;

    private List<CommentDto> comments;
}
