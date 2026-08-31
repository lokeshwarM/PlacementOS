package com.placementos.backend.domain.event;

import com.placementos.backend.domain.event.payload.GmailMessageDiscoveredPayload;
import com.placementos.backend.domain.service.GmailHistoryService;
import com.placementos.backend.domain.service.GmailMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GmailMessageDiscoveredStreamConsumerTest {

    @Mock
    private GmailMessageService gmailMessageService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOps;

    private ObjectMapper objectMapper;
    private GmailMessageDiscoveredStreamConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(redisTemplate.opsForStream()).thenReturn(streamOps);
        consumer = new GmailMessageDiscoveredStreamConsumer(gmailMessageService, redisTemplate, objectMapper);
    }

    @Test
    void onMessage_validDiscoveredEvent_callsServiceAndAcknowledges() throws Exception {
        GmailMessageDiscoveredPayload payload = new GmailMessageDiscoveredPayload("msg-101", "th-101", "cdc@example.com");
        Map<String, String> valueMap = Map.of(
                "eventType", EventType.GMAIL_MESSAGE_DISCOVERED.name(),
                "payload", objectMapper.writeValueAsString(payload)
        );

        RecordId recordId = RecordId.of("1700000000000-0");
        MapRecord<String, String, String> record = MapRecord.create(
                GmailHistoryService.REDIS_STREAM_KEY, valueMap).withId(recordId);

        consumer.onMessage(record);

        verify(gmailMessageService).retrieveAndPersistMessage("cdc@example.com", "msg-101");
        verify(streamOps).acknowledge(
                eq(GmailHistoryService.REDIS_STREAM_KEY),
                eq(GmailHistoryService.REDIS_CONSUMER_GROUP),
                eq(recordId)
        );
    }

    @Test
    void onMessage_unrelatedEventType_ignoresAndAcknowledges() {
        Map<String, String> valueMap = Map.of(
                "eventType", EventType.PLACEMENT_EMAIL_RECEIVED.name(),
                "payload", "{}"
        );

        RecordId recordId = RecordId.of("1700000000000-1");
        MapRecord<String, String, String> record = MapRecord.create(
                GmailHistoryService.REDIS_STREAM_KEY, valueMap).withId(recordId);

        consumer.onMessage(record);

        verifyNoInteractions(gmailMessageService);
        verify(streamOps).acknowledge(
                eq(GmailHistoryService.REDIS_STREAM_KEY),
                eq(GmailHistoryService.REDIS_CONSUMER_GROUP),
                eq(recordId)
        );
    }

    @Test
    void onMessage_serviceThrowsException_doesNotAcknowledge() {
        GmailMessageDiscoveredPayload payload = new GmailMessageDiscoveredPayload("msg-102", "th-102", "cdc@example.com");
        Map<String, String> valueMap = Map.of(
                "eventType", EventType.GMAIL_MESSAGE_DISCOVERED.name(),
                "payload", "{\"messageId\":\"msg-102\",\"threadId\":\"th-102\",\"sourceEmail\":\"cdc@example.com\"}"
        );

        RecordId recordId = RecordId.of("1700000000000-2");
        MapRecord<String, String, String> record = MapRecord.create(
                GmailHistoryService.REDIS_STREAM_KEY, valueMap).withId(recordId);

        doThrow(new RuntimeException("Database error"))
                .when(gmailMessageService).retrieveAndPersistMessage("cdc@example.com", "msg-102");

        consumer.onMessage(record);

        verify(streamOps, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
    }
}
