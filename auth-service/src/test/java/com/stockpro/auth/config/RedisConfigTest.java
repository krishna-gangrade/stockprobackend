package com.stockpro.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("RedisConfig unit tests")
class RedisConfigTest {

    @Test
    void stringRedisTemplate_configuresStringSerializers() {
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisConfig config = new RedisConfig();

        StringRedisTemplate template = config.stringRedisTemplate(factory);

        assertThat(template.getConnectionFactory()).isSameAs(factory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);
        assertThat(template.getValueSerializer()).isInstanceOf(StringRedisSerializer.class);
    }
}
