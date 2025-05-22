package ru.practicum.exceptions;

public class OperationUnnecessaryException extends RuntimeException {
    public OperationUnnecessaryException(final String message) {
        super(message);
    }
}
