package ru.practicum.exceptions;

public class RepeatableUserRequestException extends RuntimeException {
    public RepeatableUserRequestException(final String message) {
        super(message);
    }
}
