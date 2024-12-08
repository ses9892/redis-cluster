package com.chat.pubsub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import java.time.Duration;

import com.chat.pubsub.listener.MessageListenService;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.ClientOptions;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.TimeoutOptions;

@Configuration
public class RedisConfig {

  @Bean
  MessageListenerAdapter messageListenerAdapter() {
    return new MessageListenerAdapter(new MessageListenService());
  }

  @Bean
  RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory,
      MessageListenerAdapter listener) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisConnectionFactory);
    container.addMessageListener(listener, ChannelTopic.of("users:unregister"));
    return container;
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());
    
    return template;
  }

  @Bean
  public LettuceConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
    ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
        .enableAllAdaptiveRefreshTriggers()
        .enablePeriodicRefresh(Duration.ofSeconds(1))
        .dynamicRefreshSources(true)
        .build();

    ClusterClientOptions clientOptions = ClusterClientOptions.builder()
        .topologyRefreshOptions(topologyRefreshOptions)
        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
        .autoReconnect(true)
        .publishOnScheduler(true)
        .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5)))
        .build();

    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
        .clientOptions(clientOptions)
        .readFrom(ReadFrom.REPLICA_PREFERRED)
        .build();

    RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(redisProperties.getCluster().getNodes());
    clusterConfiguration.setMaxRedirects(3);
    
    return new LettuceConnectionFactory(clusterConfiguration, clientConfig);
  }
}
