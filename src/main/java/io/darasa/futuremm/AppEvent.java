package io.darasa.futuremm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.darasa.futuremm.socket.SocketClient;
import jakarta.annotation.PreDestroy;

@Component
public class AppEvent {
  private static final Logger LOGGER = LoggerFactory.getLogger(AppEvent.class);

  @Autowired
  private SocketClient socketClient;

  @EventListener(ApplicationReadyEvent.class)
  public void appReady() {
    socketClient.start();
  }

  @EventListener(ContextClosedEvent.class)
  public void appClosed() {
    socketClient.stop();
  }

  @PreDestroy
  private void preDestroy() {
    LOGGER.info("preDestroy");
  }

}
