package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByRequesterId(Long requesterId);

    Optional<Request> findByIdAndRequesterId(Long id, Long requesterId);

    Optional<Request> findByEventId(Long eventId);

    List<Request> findByRequesterIdAndEventId(Long requesterId, Long eventId);

    Optional<Request> findByIdAndEventId(Long id, Long eventId);

    @Query("SELECT COUNT(r) FROM Request r WHERE r.eventId = :eventId and r.status = :status")
    long countRequestsByEventAndStatus(Long eventId, RequestStatus status);

    Optional<Request> findByRequesterIdAndEventIdAndStatus(long authorId, long eventId, RequestStatus requestStatus);

    List<Request> findAllByEventIdInAndStatus(List<Long> idsList, RequestStatus status);
}
