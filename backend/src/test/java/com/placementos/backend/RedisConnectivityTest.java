package com.placementos.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class RedisConnectivityTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testRedisConnectionAndOps() {
        String key = "test:connection:key";
        String value = "hello-upstash-redis";
        
        // Write to Redis
        stringRedisTemplate.opsForValue().set(key, value);
        
        // Read from Redis
        String fetchedValue = stringRedisTemplate.opsForValue().get(key);
        
        // Assert
        assertThat(fetchedValue).isEqualTo(value);
        
        // Cleanup
        stringRedisTemplate.delete(key);
    }
}
