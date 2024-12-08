package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import java.time.Duration;

@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 클러스터 노드 변경 감지 활성화
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
            .enableAllAdaptiveRefreshTriggers()  // 모든 적응형 새로고침 트리거 활성화
            .enablePeriodicRefresh(Duration.ofSeconds(1))  // 주기적 새로고침
            .build();
        
        return template;
    }
} 