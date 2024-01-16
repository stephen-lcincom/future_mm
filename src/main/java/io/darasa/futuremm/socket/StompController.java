package io.darasa.futuremm.socket;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Controller;

import io.darasa.futuremm.property.StrategyProperties;
import lombok.Getter;

@Controller
public class StompController implements StompSessionHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(StompController.class);

  private static final String TOPIC = "/topic";
  private static final String USER_TOPIC = "/user" + TOPIC;
  private static final String TOPIC_BEST_BID = TOPIC + "/best_bid";
  private static final String TOPIC_BEST_ASK = TOPIC + "/best_ask";
  private static final String TOPIC_LAST_PRICE = TOPIC + "/index_price_update";
  private static final String TOPIC_DEPTH_UPDATE = TOPIC + "/depth_update";

  private static final String TOPIC_ORDER_UPDATE = USER_TOPIC + "/order_update";
  private static final String TOPIC_POSITION_UPDATE = USER_TOPIC + "/position_update";
  private static final String TOPIC_BALANCE_UPDATE = USER_TOPIC + "/balance_update";

  @Autowired
  @Lazy
  private SocketClient socketClient;

  @Autowired
  private StrategyProperties strategyProperties;

  @Getter
  Map<String, StompSession.Subscription> subscriptions = new HashMap<>();

  public void subscribe(StompSession stompSession, String destination) {
    LOGGER.info("Subscribing to : {}", destination);
    StompSession.Subscription subscription = stompSession.subscribe(destination, this);
    subscriptions.put(destination, subscription);
  }

  public boolean isSubscribed(String destination) {
    return subscriptions.containsKey(destination);
  }

  public void unsubscribe(String destination) {
    subscriptions.get(destination).unsubscribe();
    subscriptions.remove(destination);
    LOGGER.info("Unsubscribed {}", destination);
  }

  private String getSymbolDestination(String des) {
    return new StringBuilder(des).append("@").append(strategyProperties.getSymbol()).toString();
  }

  @Override
  public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
    subscribe(session, getSymbolDestination(TOPIC_BEST_BID));
    subscribe(session, getSymbolDestination(TOPIC_BEST_ASK));
    subscribe(session, getSymbolDestination(TOPIC_LAST_PRICE));
    subscribe(session, getSymbolDestination(TOPIC_DEPTH_UPDATE));

    List<String> userNames = connectedHeaders.get("user-name");
    if (userNames != null && !userNames.isEmpty()) {
      LOGGER.info("Successfully connected by user {}", userNames.get(0));
      subscribe(session, TOPIC_ORDER_UPDATE);
      subscribe(session, TOPIC_POSITION_UPDATE);
      subscribe(session, TOPIC_BALANCE_UPDATE);
    } else {
      LOGGER.error("Invalid token");
      System.exit(0);
    }
  }

  @Override
  public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
      Throwable exception) {
    LOGGER.info("Got an exception while handling a frame.\n" +
        "Command: {}\n" +
        "Headers: {}\n" +
        "Payload: {}\n" +
        "{}", command, headers, payload, exception);
  }

  @Override
  public void handleTransportError(StompSession session, Throwable exception) {
    LOGGER.error("Retrieved a transport error: {}", session);
    exception.printStackTrace();
  }

  @Override
  public Type getPayloadType(StompHeaders headers) {
    return String.class;
  }

  @Override
  public void handleFrame(StompHeaders headers, Object payload) {
    String destination = headers.getDestination();
    if (destination.contains(TOPIC_LAST_PRICE)) {
      Double lastPrice = Double.parseDouble(payload.toString());
      LOGGER.info("Receive last price {}", lastPrice);
    } else {
      LOGGER.warn("Unhandle destination {}", destination);
    }
  }

}