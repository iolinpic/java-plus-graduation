package ru.practicum.exceptions;


public class OperationForbiddenException extends RuntimeException {
    public OperationForbiddenException(final String message) {
        super(message);
    }
}
