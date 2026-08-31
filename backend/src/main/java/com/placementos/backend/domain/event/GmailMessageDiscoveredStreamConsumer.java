package com.placementos.backend.domain.event;

import com.placementos.backend.domain.event.payload.GmailMessageDiscoveredPayload;
import com.placementos.backend.domain.service.GmailHistoryService;
import com.placementos.backend.domain.service.GmailMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Redis Streams Consumer for {@link EventType#GMAIL_MESSAGE_DISCOVERED}.
 * Reads messages from the consumer group, retrieves Gmail messages, normalizes MIME content,
 * and durably persists the result in PostgreSQL.
 */
@Component
public class GmailMessageDiscoveredStreamConsumer implements StreamListener<String, MapRecord<String, String, String>> {

    private static final Logger log = LoggerFactory.getLogger(GmailMessageDiscoveredStreamConsumer.class);

    private final GmailMessageService gmailMessageService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public GmailMessageDiscoveredStreamConsumer(GmailMessageService gmailMessageService,
                                                RedisTemplate<String, Object> redisTemplate,
                                                ObjectMapper objectMapper) {
        this.gmailMessageService = gmailMessageService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(MapRecord<String, String, String> record) {
        try {
            Map<String, String> map = record.getValue();
            if (map == null || map.isEmpty()) {
                acknowledge(record);
                return;
            }

            String eventType = map.get("eventType");
            if (EventType.GMAIL_MESSAGE_DISCOVERED.name().equals(eventType)) {
                String payloadJson = map.get("payload");
                if (payloadJson != null && !payloadJson.isBlank()) {
                    GmailMessageDiscoveredPayload payload = objectMapper.readValue(payloadJson, GmailMessageDiscoveredPayload.class);
                    log.info("Consuming GMAIL_MESSAGE_DISCOVERED for messageId: {}, source: {}",
                            payload.getMessageId(), payload.getSourceEmail());

                    gmailMessageService.retrieveAndPersistMessage(payload.getSourceEmail(), payload.getMessageId());
                }
            }

            acknowledge(record);
        } catch (Exception e) {
            log.error("Failed to process Redis Stream record {}: {}", record.getId(), e.getMessage(), e);
            // Do not acknowledge so message can be retried or inspected in PEL
        }
    }

    private void acknowledge(MapRecord<String, String, String> record) {
        try {
            redisTemplate.opsForStream().acknowledge(
                    GmailHistoryService.REDIS_STREAM_KEY,
                    GmailHistoryService.REDIS_CONSUMER_GROUP,
                    record.getId()
            );
        } catch (Exception ex) {
            log.warn("Failed to acknowledge stream record {}: {}", record.getId(), ex.getMessage());
        }
    }
}
