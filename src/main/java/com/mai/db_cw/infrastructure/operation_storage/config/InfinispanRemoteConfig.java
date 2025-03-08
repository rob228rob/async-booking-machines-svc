package com.mai.db_cw.infrastructure.operation_storage.config;

import com.mai.db_cw.infrastructure.operation_storage.OperationStatus;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class InfinispanRemoteConfig {

    @Value("${infinispan.server.host}")
    private String infinispanServerHost;

    @Value("${infinispan.server.port}")
    private int infinispanServerPort;

    @Value("${infinispan.auth.user}")
    private String infinispanUser;

    @Value("${infinispan.auth.pass}")
    private String infinispanPass;

    @Value("${infinispan.auth.sasl-mechanism}")
    private String saslMechanism;

    @Bean
    public RemoteCacheManager remoteCacheManager() {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.addServer()
                .host(infinispanServerHost)
                .port(infinispanServerPort)
                .security()
                .authentication()
                .username(infinispanUser)
                .password(infinispanPass)
                .saslMechanism(saslMechanism);

        return new RemoteCacheManager(builder.build());
    }

    @Bean
    public RemoteCache<UUID, OperationStatus> operationsCache(RemoteCacheManager remoteCacheManager) {
        RemoteCache<UUID, OperationStatus> cache = remoteCacheManager.administration()
                .getOrCreateCache("operationsCache", "default");
        if (cache == null) {
            throw new IllegalStateException("Cache 'operationsCache' not found on remote Infinispan server");
        }
        return cache;
    }
}
