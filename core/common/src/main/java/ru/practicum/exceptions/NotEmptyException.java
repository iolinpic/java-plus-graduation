package ru.practicum.exceptions;

public class NotEmptyException extends RuntimeException {
    public NotEmptyException(final String message) {
        super(message);
    }
}
