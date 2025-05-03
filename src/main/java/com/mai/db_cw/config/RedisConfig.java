package com.mai.db_cw.config;

import com.mai.db_cw.config.infrastructure.operation_storage.RedisOperationStorage.OperationRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    @Primary
    public JedisConnectionFactory redisConnectionFactory(
            @Value("${redis.host}") String host,
            @Value("${redis.port}") int port,
            @Value("${redis.username}") String user,
            @Value("${redis.password}") String pass) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        return new JedisConnectionFactory(cfg);
    }

    @Bean("operationRecordRedisTemplate")
    public RedisTemplate<String, OperationRecord> operationRecordRedisTemplate(
            RedisConnectionFactory cf) {
        RedisTemplate<String, OperationRecord> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);

        StringRedisSerializer strSer = new StringRedisSerializer();
        tpl.setKeySerializer(strSer);
        tpl.setHashKeySerializer(strSer);

        GenericJackson2JsonRedisSerializer jsonSer = new GenericJackson2JsonRedisSerializer();
        tpl.setValueSerializer(jsonSer);
        tpl.setHashValueSerializer(jsonSer);

        tpl.afterPropertiesSet();
        return tpl;
    }

    @Bean
    public HashOperations<String, String, OperationRecord> operationRecordHashOps(
            @Qualifier("operationRecordRedisTemplate")
            RedisTemplate<String, OperationRecord> tpl) {
        return tpl.opsForHash();
    }
}