package ru.practicum.exceptions;


public class NotFoundException extends RuntimeException {
    public NotFoundException(final String message) {
        super(message);
    }
}
