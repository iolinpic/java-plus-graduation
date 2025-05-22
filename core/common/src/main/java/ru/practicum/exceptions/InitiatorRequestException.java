package ru.practicum.exceptions;

public class InitiatorRequestException extends RuntimeException {
    public InitiatorRequestException(final String message) {
        super(message);
    }
}
