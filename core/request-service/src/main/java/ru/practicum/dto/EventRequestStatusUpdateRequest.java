package ru.practicum.dto;

import lombok.Data;
import ru.practicum.dto.request.RequestStatus;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {

    private List<Long> requestIds;

    private RequestStatus status;

}
