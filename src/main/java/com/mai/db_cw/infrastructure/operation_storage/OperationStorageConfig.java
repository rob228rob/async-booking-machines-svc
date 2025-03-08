package com.mai.db_cw.infrastructure.operation_storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OperationStorageConfig {

    @Bean
    public OperationStorage operationStorage() {
        return new SimpleInMemoryOperationStorage();
    }
}
