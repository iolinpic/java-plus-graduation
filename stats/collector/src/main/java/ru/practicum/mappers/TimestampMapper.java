package ru.practicum.mappers;

import com.google.protobuf.Timestamp;

import java.time.Instant;

public class TimestampMapper {
    public static Instant mapToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
