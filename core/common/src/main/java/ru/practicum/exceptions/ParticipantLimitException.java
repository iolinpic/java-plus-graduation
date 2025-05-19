package ru.practicum.exceptions;

public class ParticipantLimitException extends RuntimeException {
    public ParticipantLimitException(final String message) {
        super(message);
    }
}
