package com.placementos.backend.config;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.ObjectMapper;

public class CustomJsonRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper mapper;

    public CustomJsonRedisSerializer() {
        this.mapper = new ObjectMapper();
    }

    @Override
    public byte[] serialize(Object t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        try {
            return mapper.writeValueAsBytes(t);
        } catch (Exception ex) {
            throw new SerializationException("Could not write JSON: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return mapper.readValue(bytes, Object.class);
        } catch (Exception ex) {
            throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
        }
    }
}
