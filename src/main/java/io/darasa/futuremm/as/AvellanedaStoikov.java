package io.darasa.futuremm.as;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.darasa.futuremm.entity.MMConfig;
import io.darasa.futuremm.entity.MMHistory;
import io.darasa.futuremm.enums.Strategy;

public class AvellanedaStoikov extends MM {
  private static final Logger LOGGER = LoggerFactory.getLogger(AvellanedaStoikov.class);
  public static final Strategy STRATEGY = Strategy.AVELLANEDA_STOIKOV;
  private double q; // inventory quantity
  private final double qMax; // max inventory quantity
  private final double T = 1; // terminal time
  private double t = 0; // current time
  private long currentCycle;
  private double a; // alpha
  private double b; // beta
  private double d; // delta
  private double da; // delta a, distance between ask price and mid price
  private double db; // delta b, distance between mid price and bid price
  private double y; // gammar, inventory risk
  private double o; // sigma, market volatility
  private final double w; // omega, upper bound the inventory position
  private double _0; // theta
  private double s; // mid price
  private double dt;
  private double k; // density of market order size
  private double L; // lambda, frequency of market buy or sell orders
                    // = total volume traded / average size of market orders per day
  private double A = L / a;
  private double x; // initial balance ($)
  private double pa; // ask price
  private double pb; // bid price
  private double na; // number of tokens sold
  private double nb; // number of tokens bought
  private ScheduledFuture<?> runner;
  private boolean isSkip;

  // @lombok.Builder
  // public static class Builder {
  // private Double initBalance;

  // AvellanedaStoikov build() {
  // return new AvellanedaStoikov(initBalance, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  // }
  // }

  public AvellanedaStoikov(MMConfig mmConfig, ApplicationContext appContext) {
    super(mmConfig, appContext);
    qMax = 100;
    o = 2;
    q = 0;
    y = 0.1;
    k = 1.5;
    w = omega();
    // this.qMax = qMax;
    // w = Math.pow(y * o, 2) * Math.pow(qMax + 1, 2) / 2;
  }

  private double omega() {
    return Math.pow(y * o * (qMax + 1), 2) / 2;
  }

  // public static AvellanedaStoikov newInstance(double initBalance ) {
  // return new AvellanedaStoikov();
  // }

  private double value(double x) {
    return -Math.exp(-y * x) * Math.exp(-y * q * s) * Math.exp((Math.pow(y * q * o, 2) * (T - t)) / 2);
  }

  private double reservationPrice() {
    return s - q * y * Math.pow(o, 2) * (T - t);
  }

  private double reservationAskPrice() {
    return s + (1 - 2 * q) * y * o * o * (T - t) / 2;
  }

  private double reservationBidPrice() {
    return s + (-1 - 2 * q) * y * o * o * (T - t) / 2;
  }

  private double infRsvnAskPrice() {
    return s + (1 / y) * Math.log(1 + ((1 - 2 * q) * Math.pow(y * o, 2)) / (2 * w - Math.pow(y * q * o, 2)));
  }

  private double infRsvnBidPrice() {
    return s + (1 / y) * Math.log(1 + ((-1 - 2 * q) * Math.pow(y * o, 2)) / (2 * w - Math.pow(y * q * o, 2)));
  }

  private double symetricBidPrice(double midPrice, double spread) {
    return midPrice - spread / 2;
  }

  private double symetricAskPrice(double midPrice, double spread) {
    return midPrice + spread / 2;
  }

  // the weath in cash at time t
  private double dXt() {
    return pa * na - pb * nb;
  }

  // number of tokens held at time t
  private double qt() {
    return nb - na;
  }

  private double optimalSpread() {
    return y * Math.pow(o, 2) * (T - t) + 2 * Math.log(1 + y / k) / y;
  }

  @Override
  public void internalStart() {
    LOGGER.info("Starting MM {} with configuration:", mmConfig.getId());
    LOGGER.info("qMax {}", qMax);
    LOGGER.info("w {}", w);
    LOGGER.info("s {}", s);
    LOGGER.info("o {}", o);
    LOGGER.info("q {}", q);
    LOGGER.info("y {}", y);
    LOGGER.info("k {}", k);
    LOGGER.info("lastPrice {}", lastPrice);
    LOGGER.info("spread {}", spread);
    dt = T / nCycles;
    runner = taskScheduler.scheduleAtFixedRate((Runnable) this::beginCycle, Duration.ofSeconds(1));
    isRunning = true;
  }

  @Override
  public void internalStop() {
    runner.cancel(false);
    isRunning = runner.isCancelled();
    emitStopped();
  }

  @Override
  public void beginCycle() {
    if (isSkip) {
      return;
    }

    if (currentCycle > 0) {
      s = this.isSimulation() ? randomMidPrice() : midPrice;
    } else {
      s = midPrice;
    }

    double reservationPrice = reservationPrice();
    double optimalSpread = optimalSpread();
    double symetricBidPrice = symetricBidPrice(s, optimalSpread);
    double symetricAskPrice = symetricAskPrice(s, optimalSpread);
    double inventoryBidPrice = symetricBidPrice(reservationPrice, optimalSpread);
    double inventoryAskPrice = symetricAskPrice(reservationPrice, optimalSpread);
    double reservationBidPrice = reservationBidPrice();
    double reservationAskPrice = reservationAskPrice();
    double infRsvnBidPrice = infRsvnBidPrice();
    double infRsvnAskPrice = infRsvnAskPrice();
    double orderSizeOBSpread = orderSize(spread);
    double orderSizeOptimalSpread = orderSize(optimalSpread);
    mmHistoryService.saveDb(new MMHistory(mmConfig.getId(), List.of(
        t, // 0
        currentCycle, // 1
        lastPrice, // 2
        s, // 3
        reservationPrice, // 4
        symetricBidPrice, // 5
        symetricAskPrice, // 6
        reservationBidPrice, // 7
        reservationAskPrice, // 8
        infRsvnBidPrice, // 9
        infRsvnAskPrice, // 10
        spread, // 11
        orderSizeOBSpread, // 12
        optimalSpread, // 13
        orderSizeOptimalSpread, // 14
        q, // 15
        inventoryBidPrice, // 16
        inventoryAskPrice // 17
    )));

    q = (random.nextBoolean() ? 1 : -1) * Math.random();
    currentCycle += 1;
    t += dt;
    if (currentCycle > nCycles) {
      internalStop();
      return;
    }
  }

  private double randomMidPrice() {
    return s + (random.nextBoolean() ? 1 : -1) * o * Math.sqrt(dt);
  }

  // the optimal amount to be lowered in the bid price should be linearly
  // proportional to the degree of BTC’s inventory excess, which makes perfect
  // intuitive sense.
  private double orderSize(double spread) {
    return Math.sqrt(y * lastPrice * spread);
  }

}
