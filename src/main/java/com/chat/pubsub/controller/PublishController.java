package com.chat.pubsub.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import org.springframework.data.redis.connection.RedisConnection;

@Slf4j
@RestController
@RequestMapping("/publish")
public class PublishController {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // 각 마스터 노드별 채널 정의
    private static final String MASTER1_CHANNEL = "{master1}users:unregister";
    private static final String MASTER2_CHANNEL = "{master2}users:unregister";

    public PublishController(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Master1 노드로 메시지 발행
     * 해시태그 {master1}을 사용하여 특정 마스터 노드로 라우팅
     */
    @PostMapping("/master1/events/users/deregister")
    public ResponseEntity<?> publishToMaster1(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        return publishMessage(MASTER1_CHANNEL, message, "Master1");
    }

    /**
     * Master2 노드로 메시지 발행
     * 해시태그 {master2}를 사용하여 특정 마스터 노드로 라우팅
     */
    @PostMapping("/master2/events/users/deregister")
    public ResponseEntity<?> publishToMaster2(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        return publishMessage(MASTER2_CHANNEL, message, "Master2");
    }

    /**
     * 메시지 발행 공통 로직
     * @param channel 발행할 채널
     * @param message 발행할 메시지
     * @param masterNode 마스터 노드 식별자 (로깅용)
     */
    private ResponseEntity<?> publishMessage(String channel, String message, String masterNode) {
        try {
            redisTemplate.convertAndSend(channel, message);
            log.info("Successfully published message to {} node", masterNode);
            return ResponseEntity.ok().build();
            
        } catch (RedisConnectionFailureException e) {
            log.error("{} node connection failed. Retrying...", masterNode, e);
            
            // 레디스 연결 실패 시 재시도
            try {
                Thread.sleep(1000); // 1초 대기 후 재시도
                redisTemplate.convertAndSend(channel, message);
                log.info("Retry successful for {} node", masterNode);
                return ResponseEntity.ok().build();
                
            } catch (Exception retryEx) {
                log.error("{} node retry failed", masterNode, retryEx);
                return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse(masterNode + " node is temporarily unavailable"));
            }
            
        } catch (Exception e) {
            log.error("Failed to publish message to {} node", masterNode, e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to publish message to " + masterNode + ": " + e.getMessage()));
        }
    }

        @PostMapping("/data")
    public ResponseEntity<?> setData(@RequestBody Map<String, String> payload) {
        try {
            String key = payload.get("key");
            String value = payload.get("value");
            redisTemplate.opsForValue().set(key, value);
            log.info("Successfully saved data with key: {}", key);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to save data", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to save data: " + e.getMessage()));
        }
    }

    @GetMapping("/data/{key}")
    public ResponseEntity<?> getData(@PathVariable("key") String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("value", value);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to get data", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to get data: " + e.getMessage()));
        }
    }

    @GetMapping("/cluster/info")
    public ResponseEntity<?> getClusterInfo() {
        try {
            Map<String, Object> clusterInfo = redisTemplate.execute((RedisConnection connection) -> {
                Properties clusterProps = connection.info("cluster");
                Properties replicationProps = connection.info("replication");
                Properties serverProps = connection.info("server");
                
                // 디버깅을 위한 전체 정보 출력
                log.info("=== Redis Cluster Info ===");
                clusterProps.forEach((k, v) -> log.info("Cluster - {}: {}", k, v));
                replicationProps.forEach((k, v) -> log.info("Replication - {}: {}", k, v));
                
                Map<String, Object> result = new HashMap<>();
                // 클러스터 정보
                result.put("nodeId", clusterProps.getProperty("cluster_my_id"));
                result.put("clusterState", clusterProps.getProperty("cluster_state"));
                result.put("clusterSlotsAssigned", clusterProps.getProperty("cluster_slots_assigned"));
                result.put("clusterSlotsOk", clusterProps.getProperty("cluster_slots_ok"));
                
                // 복제 정보
                result.put("role", replicationProps.getProperty("role"));
                result.put("connectedSlaves", replicationProps.getProperty("connected_slaves"));
                
                // 서버 정보
                result.put("redis_version", serverProps.getProperty("redis_version"));
                result.put("tcp_port", serverProps.getProperty("tcp_port"));
                result.put("uptime_in_seconds", serverProps.getProperty("uptime_in_seconds"));
                
                if ("slave".equals(replicationProps.getProperty("role"))) {
                    result.put("masterHost", replicationProps.getProperty("master_host"));
                    result.put("masterPort", replicationProps.getProperty("master_port"));
                    result.put("masterLinkStatus", replicationProps.getProperty("master_link_status"));
                }
                
                return result;
            });
            
            return ResponseEntity.ok(clusterInfo);
            
        } catch (Exception e) {
            log.error("Failed to get cluster info", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to get cluster info: " + e.getMessage()));
        }
    }
}

@Data
@AllArgsConstructor
class ErrorResponse {
    private String message;
}
