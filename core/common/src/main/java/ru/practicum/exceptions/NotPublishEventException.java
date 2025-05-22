package ru.practicum.exceptions;

public class NotPublishEventException extends RuntimeException {
    public NotPublishEventException(final String message) {
        super(message);
    }
}
