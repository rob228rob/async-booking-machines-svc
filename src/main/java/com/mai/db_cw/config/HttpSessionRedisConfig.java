package com.mai.db_cw.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;

import java.time.Duration;

/**
 * @author batoyan.rl
 * @since 03.05.2025
 */
@Configuration
@EnableRedisHttpSession(
        maxInactiveIntervalInSeconds = 60 * 60)// 1 час
public class HttpSessionRedisConfig {

//    @Bean
//    public static ConfigureRedisAction configureRedisAction() {
//        return ConfigureRedisAction.;
//    }

    /**
     * Специальный RedisTemplate, который будет хранить
     * значения (и hash‐значения) не как String, а через JDK‐сериализацию.
     */
    @Bean
    public RedisTemplate<String, Object> springSessionRedisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(redisConnectionFactory);

        StringRedisSerializer keySer = new StringRedisSerializer();
        tpl.setKeySerializer(keySer);
        tpl.setHashKeySerializer(keySer);

        JdkSerializationRedisSerializer valSer = new JdkSerializationRedisSerializer();
        tpl.setValueSerializer(valSer);
        tpl.setHashValueSerializer(valSer);

        tpl.afterPropertiesSet();
        return tpl;
    }

    @Bean
    public RedisIndexedSessionRepository sessionRepository(
            RedisTemplate<String, Object> springSessionRedisTemplate) {
        RedisIndexedSessionRepository repo =
                new RedisIndexedSessionRepository(springSessionRedisTemplate);
        repo.setDefaultMaxInactiveInterval(Duration.ofHours(2));
        return repo;
    }
}
