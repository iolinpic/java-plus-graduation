package ru.practicum.dto.comment;

import lombok.Data;

@Data
public class CommentDto {
    private long id;
    private String text;

    private long eventId;

    private long authorId;

    private String created;

    private String status;
}
