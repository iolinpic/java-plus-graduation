package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.client.CollectorClient;
import ru.practicum.dto.EventRequestStatusUpdateRequest;
import ru.practicum.dto.EventRequestStatusUpdateResult;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

@RestController
@RequestMapping(path = "/users/{userId}")
@RequiredArgsConstructor
public class PrivateRequestController {

    private final RequestService requestService;
    private final CollectorClient collectorClient;

    /**
     * Получение информации о заявках текущего пользователя на участие в чужих событиях
     * В случае, если по заданным фильтрам не найдено ни одной заявки, возвращает пустой список.
     *
     * @param userId id текущего пользователя
     */
    @GetMapping("/requests")
    public List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId, HttpServletRequest request) {
        return requestService.getUserRequests(userId, request);
    }

    /**
     * Добавление запроса от текущего пользователя на участие в событии.
     * Нельзя добавить повторный запрос  (Код ошибки 409).
     * Инициатор события не может добавить запрос на участие в своём событии (Код ошибки 409).
     * Нельзя участвовать в неопубликованном событии (Код ошибки 409).
     * Если у события достигнут лимит запросов на участие - необходимо вернуть ошибку (Код ошибки 409).
     * Если для события отключена пре-модерация запросов на участие, то запрос должен автоматически перейти
     * в состояние подтвержденного.
     *
     * @param userId  id текущего пользователя
     * @param eventId id события
     */
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addParticipationRequest(@PathVariable Long userId,
                                                           @RequestParam Long eventId) {
        ParticipationRequestDto result = requestService.addParticipationRequest(userId, eventId);
        collectorClient.sendEventRegistration(userId, eventId);
        return result;
    }

    /**
     * Отмена своего запроса на участие в событии.
     *
     * @param userId    id текущего пользователя
     * @param requestId id запроса на участие
     */
    @PatchMapping("/requests/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(@PathVariable Long userId, @PathVariable Long requestId) {
        return requestService.cancelRequest(userId, requestId);
    }

    /**
     * Получение информации о запросах на участие в событии текущего пользователя.
     * В случае, если по заданным фильтрам не найдено ни одной заявки, возвращает пустой список
     *
     * @param userId  id текущего пользователя
     * @param eventId id события
     * @return Найденые запросы на участие в событии текущего пользователя.
     */
    @GetMapping("/events/{eventId}/requests")
    public List<ParticipationRequestDto> getEventParticipants(@PathVariable Long userId, @PathVariable Long eventId,
                                                              HttpServletRequest request) {
        return requestService.getEventParticipants(userId, eventId, request);
    }

    /**
     * Изменение статуса (подтверждена, отменена) заявок на участие в событии текущего пользователя.
     * Если для события лимит заявок равен 0 или отключена пре-модерация заявок, то подтверждение заявок не требуется.
     * Нельзя подтвердить заявку, если уже достигнут лимит по заявкам на данное событие (Код ошибки 409).
     * Статус можно изменить только у заявок, находящихся в состоянии ожидания.
     * Если при подтверждении данной заявки, лимит заявок для события исчерпан, то все неподтверждённые заявки
     * необходимо отклонить
     *
     * @param userId            id текущего пользователя
     * @param eventId           id события текущего пользователя
     * @param eventStatusUpdate новый статус для заявок на участие в событии текущего пользователя
     * @return Результат подтверждения/отклонения заявок на участие в событии
     */
    @PatchMapping("/events/{eventId}/requests")
    public EventRequestStatusUpdateResult changeRequestStatus(@PathVariable Long userId, @PathVariable Long eventId,
                                                              @RequestBody EventRequestStatusUpdateRequest eventStatusUpdate,
                                                              HttpServletRequest request) {
        return requestService.changeRequestStatus(userId, eventId, eventStatusUpdate, request);
    }

}
