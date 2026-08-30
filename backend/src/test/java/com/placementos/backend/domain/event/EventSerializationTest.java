package com.placementos.backend.domain.event;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void eventEnvelope_serializesAndDeserializesCorrectly() throws Exception {
        Map<String, String> payload = Map.of("documentId", "doc-123", "studentId", "stu-456");
        EventEnvelope<Map<String, String>> originalEvent = new EventEnvelope<>(
                EventType.DOCUMENT_PROCESS_REQUESTED,
                "gmail-ingestion",
                payload
        );

        // Verify ID is generated
        assertThat(originalEvent.getEventId()).isNotNull();
        assertThat(originalEvent.getTimestamp()).isNotNull();

        String json = objectMapper.writeValueAsString(originalEvent);
        
        // Assert JSON contains the expected fields
        assertThat(json).contains("DOCUMENT_PROCESS_REQUESTED");
        assertThat(json).contains("gmail-ingestion");
        assertThat(json).contains("doc-123");

        EventEnvelope<Map<String, String>> deserializedEvent = objectMapper.readValue(
                json,
                new TypeReference<EventEnvelope<Map<String, String>>>() {}
        );

        assertThat(deserializedEvent.getEventId()).isEqualTo(originalEvent.getEventId());
        assertThat(deserializedEvent.getEventType()).isEqualTo(EventType.DOCUMENT_PROCESS_REQUESTED);
        assertThat(deserializedEvent.getTimestamp()).isEqualTo(originalEvent.getTimestamp());
        assertThat(deserializedEvent.getSource()).isEqualTo("gmail-ingestion");
        assertThat(deserializedEvent.getPayload()).isEqualTo(payload);
    }
}
