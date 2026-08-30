package com.placementos.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedisConfigTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads_andRedisTemplateBeanIsPresent() {
        // This test ensures the application can boot and auto-configure
        // even if Redis is not actually contacted (since Spring Boot connections are lazy).
        assertThat(context.containsBean("redisTemplate")).isTrue();
        
        RedisTemplate<?, ?> template = context.getBean("redisTemplate", RedisTemplate.class);
        assertThat(template).isNotNull();
    }
}
