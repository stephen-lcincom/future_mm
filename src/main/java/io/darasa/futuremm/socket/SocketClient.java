package io.darasa.futuremm.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import io.darasa.futuremm.property.SocketProperties;

@Component
public class SocketClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(SocketClient.class);

  @Autowired
  private SocketProperties socketProperties;

  @Autowired
  private StompController stompController;

  private StompSession stompSession;
  private boolean isReconnecting;

  public void start() {
    WebSocketClient client = new StandardWebSocketClient();
    WebSocketStompClient stompClient = new WebSocketStompClient(client);
    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    try {
      String url = socketProperties.getUrl();
      LOGGER.info("Connecting to WebSocket {}", url);
      stompSession = stompClient
          .connectAsync(url, stompController)
          .get();
    } catch (Exception e) {
      LOGGER.error("Connection failed", e);
    }
  }

  public void stop() {
    try {
      if (stompSession != null) {
        stompSession.disconnect();
      }
      LOGGER.info("Disconnected");
    } catch (Exception e) {
      LOGGER.warn("Try to stop socket client failed", e);
    } finally {
      stompSession = null;
    }
  }

  public void reconnect() {
    if (isReconnecting) {
      LOGGER.debug("Skip reconnection");
      return;
    }

    LOGGER.debug("Reconnecting");
    isReconnecting = true;
    stop();
    start();
    isReconnecting = false;
  }

  public boolean isConnected() {
    return stompSession == null ? false : stompSession.isConnected();
  }

  // @Scheduled(fixedRate = DEFAULT_INTERVAL, initialDelay = DEFAULT_INTERVAL)
  // private void checkConnectionStatus() {
  // if (!isConnected()) {
  // reconnect();
  // }
  // }
}
