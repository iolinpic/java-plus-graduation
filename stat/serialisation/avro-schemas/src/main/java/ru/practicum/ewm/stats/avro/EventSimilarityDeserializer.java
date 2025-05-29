package ru.practicum.ewm.stats.avro;

public class EventSimilarityDeserializer extends BaseAvroDeserializer<EventSimilarityAvro>{
    public EventSimilarityDeserializer() {super(EventSimilarityAvro.getClassSchema());}
}
