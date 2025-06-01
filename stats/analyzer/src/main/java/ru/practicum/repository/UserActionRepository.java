package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.models.UserAction;

import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {
    List<UserAction> findByUserId(Long userId);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    List<UserAction> findByEventId(Long eventId);

    List<UserAction> findByUserIdAndEventId(Long userId, Long eventId);
}
