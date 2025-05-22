package ru.practicum.dto.request;

import lombok.Data;

@Data
public class ParticipationRequestDto {

    private Long id;

    private Long event;

    private Long requester;

    private String created;

    private String status;

}
