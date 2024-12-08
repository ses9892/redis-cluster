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

/**
 * Redis 클러스터 연결 및 설정을 관리하는 Configuration 클래스
 */
@Configuration
public class RedisConfig {

  /**
   * Redis 메시지 수신을 위한 리스너 어댑터를 생성
   * MessageListenService를 통해 실제 메시지 처리를 수행
   */
  @Bean
  MessageListenerAdapter messageListenerAdapter() {
    return new MessageListenerAdapter(new MessageListenService());
  }

  /**
   * Redis 메시지 리스너 컨테이너 설정
   * - 메시지 수신을 위한 컨테이너를 생성하고 연결 설정
   * - "users:unregister" 채널에 대한 구독 설정
   */
  @Bean
  RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory,
      MessageListenerAdapter listener) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(redisConnectionFactory);
    container.addMessageListener(listener, ChannelTopic.of("users:unregister"));
    return container;
  }

  /**
   * Redis 템플릿 설정
   * - Redis 데이터 접근을 위한 템플릿 구성
   * - 모든 키와 값에 대해 String 직렬화 사용
   */
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    
    // 모든 키-값에 대해 String 직렬화 설정
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new StringRedisSerializer());
    
    return template;
  }

  /**
   * Lettuce Redis 클라이언트 연결 팩토리 설정
   * @param redisProperties Redis 속성 정보
   */
  @Bean
  public LettuceConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
    // 클러스터 토폴로지 갱신 옵션 설정
    ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
        .enableAllAdaptiveRefreshTriggers()    // 모든 적응형 갱신 트리거 활성화
        .enablePeriodicRefresh(Duration.ofSeconds(1))    // 1초마다 주기적 갱신
        .dynamicRefreshSources(true)    // 동적 갱신 소스 활성화
        .build();

    // 클러스터 클라이언트 옵션 설정
    ClusterClientOptions clientOptions = ClusterClientOptions.builder()
        .topologyRefreshOptions(topologyRefreshOptions)
        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)    // 연결 끊김 시 명령 거부
        .autoReconnect(true)    // 자동 재연결 활성화
        .publishOnScheduler(true)    // 스케줄러에 발행 활성화
        .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(5)))    // 5초 타임아웃 설정
        .build();

    // Lettuce 클라이언트 설정
    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
        .clientOptions(clientOptions)
        .readFrom(ReadFrom.REPLICA_PREFERRED)    // 복제본 우선 읽기 설정
        .build();

    // Redis 클러스터 설정
    RedisClusterConfiguration clusterConfiguration = new RedisClusterConfiguration(redisProperties.getCluster().getNodes());
    clusterConfiguration.setMaxRedirects(3);    // 최대 리다이렉트 횟수 설정
    
    return new LettuceConnectionFactory(clusterConfiguration, clientConfig);
  }
}
