package com.chat.pubsub.listener;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MessageListenService implements MessageListener {

  @Override
  public void onMessage(Message message, @Nullable byte[] pattern) {
    log.info("Received {} , message: {}", new String(message.getChannel()), new String(message.getBody()));
  }
}
