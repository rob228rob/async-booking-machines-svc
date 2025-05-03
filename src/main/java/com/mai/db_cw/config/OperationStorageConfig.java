package com.mai.db_cw.config;

import com.mai.db_cw.config.infrastructure.operation_storage.OperationStorage;
import com.mai.db_cw.config.infrastructure.operation_storage.RedisOperationStorage;
import com.mai.db_cw.config.infrastructure.operation_storage.SimpleInMemoryOperationStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.HashOperations;

@Configuration
public class OperationStorageConfig {

    public OperationStorage operationStorage() {
        return new SimpleInMemoryOperationStorage();
    }

    @Bean
    @Primary
    public OperationStorage operationStorage(HashOperations<String, String, RedisOperationStorage.OperationRecord> hashOperations) {
        return new RedisOperationStorage(hashOperations);
    }
}
