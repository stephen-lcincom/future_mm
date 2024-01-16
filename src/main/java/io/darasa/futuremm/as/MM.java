package io.darasa.futuremm.as;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import io.darasa.futuremm.entity.MMConfig;
import io.darasa.futuremm.service.MMHistoryService;
import io.darasa.futuremm.service.MMService;
import lombok.Getter;

public abstract class MM {
  private static final Logger LOGGER = LoggerFactory.getLogger(MM.class);
  final TaskScheduler taskScheduler;
  final MMHistoryService mmHistoryService;
  final MMService mmService;
  final MMUserService mmUserService;
  final OrderController orderController;
  private final GameCenter gameCenter;
  final Random random = new Random();
  @Getter
  final MMConfig mmConfig;
  double midPrice;
  double lastPrice;
  private double bestAskPrice;
  private double bestBidPrice;
  double spread;
  private double volatility;
  private double density;
  private long nextFundingTime;
  boolean isRunning;
  long nCycles;
  @Getter
  private boolean simulation;
  final MMUser mmUser;

  public MM(MMConfig mmConfig, ApplicationContext appContext) {
    this.mmConfig = mmConfig;
    taskScheduler = appContext.getBean(Constant.SCHEDULER_NAME, ThreadPoolTaskScheduler.class);
    mmHistoryService = appContext.getBean(MMHistoryService.class);
    mmService = appContext.getBean(MMService.class);
    mmUserService = appContext.getBean(MMUserService.class);
    orderController = appContext.getBean(OrderController.class);
    gameCenter = appContext.getBean(GameCenter.class);
    mmUser = mmUserService.getUser(mmConfig.getUserKey());
    if (mmUser == null) {
      throw new AppException(HttpMessage.NOT_FOUND);
    }
  }

  abstract public void beginCycle();

  abstract public void internalStart();

  abstract public void internalStop();

  public void start(boolean simulation) {
    this.simulation = simulation;

    OrderBook orderBook = gameCenter.getOrderBook(mmConfig.getSymbol());
    updateBestBidAsk(orderBook.getBestBid(), orderBook.getBestAsk());
    updateLastPrice(orderBook.getLastPrice());

    LOGGER.info("Starting {}", mmConfig.getId());
    updateNCycles();
    internalStart();
  }

  private void updateNCycles() {
    if (mmConfig.getNCycles() != null) {
      nCycles = mmConfig.getNCycles();
    } else if (mmConfig.getEndTime() != null) {
      nCycles = Instant.now().until(Instant.ofEpochMilli(mmConfig.getEndTime()), ChronoUnit.SECONDS);
    } else {
      throw new AppException(HttpMessage.CYCLE_DURATION_SMALL);
    }
  }

  public void stop() {
    LOGGER.info("Stopping {}", mmConfig.getId());
    internalStop();
  }

  public void restart(boolean simulation) {
    LOGGER.info("Restart {}", mmConfig.getId());
    stop();
    start(simulation);
  }

  public void updateLastPrice(double price) {
    lastPrice = price;
    if (midPrice == 0) {
      midPrice = lastPrice * 1.01;
    }
  }

  public void updateBestBidAsk(double bidPrice, double askPrice) {
    bestBidPrice = bidPrice;
    bestAskPrice = askPrice;
    updateMidPrice();
  }

  public void updateBestBidPrice(double price) {
    bestBidPrice = price;
    updateMidPrice();
  }

  public void updateBestAskPrice(double price) {
    bestAskPrice = price;
    updateMidPrice();
  }

  private void updateMidPrice() {
    midPrice = (bestAskPrice + bestBidPrice) / 2;
    spread = bestAskPrice - bestBidPrice;
  }

  public void updateNextFundingTime(long time) {
    nextFundingTime = time;
  }

  public boolean isRunning() {
    return isRunning;
  }

  void emitStopped() {
    mmService._updateStatus(mmConfig.getId(), MMConfigStatus.STOPPED);
    LOGGER.info("MM {} stopped", mmConfig.getId());
  }

}
