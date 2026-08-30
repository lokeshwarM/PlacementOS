package com.placementos.backend.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base envelope for asynchronous events and jobs.
 * This ensures a stable contract between producers (Spring Boot/Gmail)
 * and consumers (Workers/Python).
 */
public class EventEnvelope<T> {

    private String eventId;
    private EventType eventType;
    private String timestamp;
    private String source;
    private T payload;

    public EventEnvelope() {
    }

    public EventEnvelope(EventType eventType, String source, T payload) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = Instant.now().toString();
        this.source = source;
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
