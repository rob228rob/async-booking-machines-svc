package com.mai.db_cw.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.UUID;

@Configuration
public class RedisConfig {

    @Bean
    public JedisConnectionFactory redisConnectionFactory(@Value("${redis.host}") String redisHost,
                                                         @Value("${redis.port}") int redisPort,
                                                         @Value("${redis.username}") String username,
                                                         @Value("${redis.password}") String password) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setPassword(password);
        config.setUsername(username);
        return new JedisConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, UUID> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String,  UUID> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }
}