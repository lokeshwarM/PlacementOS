package com.placementos.backend.config;

import com.placementos.backend.domain.event.GmailMessageDiscoveredStreamConsumer;
import com.placementos.backend.domain.service.GmailHistoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serialization for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use Jackson 3 for values to support JSON serialization of generic event envelopes
        CustomJsonRedisSerializer jsonSerializer = new CustomJsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            GmailMessageDiscoveredStreamConsumer consumer) {

        // Ensure stream and consumer group exist
        try {
            connectionFactory.getConnection().streamCommands().xGroupCreate(
                    GmailHistoryService.REDIS_STREAM_KEY.getBytes(StandardCharsets.UTF_8),
                    GmailHistoryService.REDIS_CONSUMER_GROUP,
                    ReadOffset.from("0-0"),
                    true
            );
        } catch (Exception e) {
            // Already created or BUSYGROUP, safely ignore
        }

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofSeconds(2))
                        .serializer(new StringRedisSerializer())
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container =
                StreamMessageListenerContainer.create(connectionFactory, options);

        container.receive(
                Consumer.from(GmailHistoryService.REDIS_CONSUMER_GROUP, "placementos-backend-worker-1"),
                StreamOffset.create(GmailHistoryService.REDIS_STREAM_KEY, ReadOffset.lastConsumed()),
                consumer
        );

        return container;
    }
}
