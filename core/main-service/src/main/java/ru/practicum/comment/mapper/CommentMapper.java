package ru.practicum.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.model.Comment;
import ru.practicum.events.model.Event;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", source = "event")
    Comment toComment(NewCommentDto newCommentDto, Event event);

    @Mapping(target = "created", dateFormat = "yyyy-MM-dd HH:mm:ss")
    @Mapping(target = "eventId", expression = "java(comment.getEvent().getId())")
    @Mapping(target = "status", expression = "java(comment.getStatus().name())")
    CommentDto toDto(Comment comment);

}
