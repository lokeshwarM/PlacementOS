package com.placementos.backend.config;

import com.placementos.backend.domain.event.EventEnvelope;
import com.placementos.backend.domain.event.EventType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.url", () -> "redis://" + redis.getHost() + ":" + redis.getFirstMappedPort());
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void actualRedisConnection_canWriteAndReadEvent() {
        EventEnvelope<Map<String, String>> event = new EventEnvelope<>(
                EventType.PLACEMENT_EMAIL_RECEIVED,
                "test-source",
                Map.of("messageId", "MSG-999")
        );

        String key = "test:event:" + event.getEventId();

        // Write to Redis
        redisTemplate.opsForValue().set(key, event, 5, TimeUnit.SECONDS);

        // Read from Redis
        Object retrieved = redisTemplate.opsForValue().get(key);

        assertThat(retrieved).isNotNull();
        // The value is retrieved as a LinkedHashMap because of generic type erasure 
        // in JSON deserialization, but we can verify the payload structure.
        assertThat(retrieved.toString()).contains("PLACEMENT_EMAIL_RECEIVED");
        assertThat(retrieved.toString()).contains("MSG-999");
    }
}
