package com.dasara.longshort.service;

import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import com.dasara.longshort.AppConfig;
import com.dasara.longshort.constaint.Currency;
import com.dasara.longshort.constaint.MMConfigStatus;
import com.dasara.longshort.dto.MMDto;
import com.dasara.longshort.dto.TokenResponseDto.User;
import com.dasara.longshort.dwhs.SearchResult;
import com.dasara.longshort.entity.MMConfig;
import com.dasara.longshort.entity.MMUser;
import com.dasara.longshort.entity.SymbolConfig;
import com.dasara.longshort.exception.AppException;
import com.dasara.longshort.game.FundingManager.FundingIndex;
import com.dasara.longshort.game.GameCenter;
import com.dasara.longshort.game.OrderBook;
import com.dasara.longshort.messaging.InternalChannel;
import com.dasara.longshort.mm.ASParams;
import com.dasara.longshort.mm.AvellanedaStoikov;
import com.dasara.longshort.mm.EndTimeStrategy;
import com.dasara.longshort.mm.MM;
import com.dasara.longshort.mm.Strategy;
import com.dasara.longshort.others.Helper;
import com.dasara.longshort.repository.MMConfigRepository;
import com.dasara.longshort.rest.model.HttpResponse;
import com.dasara.longshort.rest.model.HttpResponse.HttpMessage;

@Service
@ConditionalOnExpression(AppConfig.CONDITION_RUN_GAME)
public class MMService extends DB<MMConfig> {
  private static final String REDIS_KEY = "mm_config:";
  private static final Logger LOGGER = LoggerFactory.getLogger(MMService.class);

  @Autowired
  private MMConfigRepository mmRepository;

  @Autowired
  private GameCenter gameCenter;

  @Autowired
  private TaskScheduler taskScheduler;

  @Autowired
  private ApplicationContext appContext;

  @Autowired
  private MMUserService mmUserService;

  @Autowired
  private SymbolConfigService symbolConfigService;

  private long numberOfMMs;

  private Map<String, Map<String, MM>> symbolMMId = new HashMap<>();
  private Map<String, List<String>> uidMMIds = new HashMap<>();

  @Override
  public MMConfig saveDb(MMConfig data) {
    return mmRepository.save(data);
  }

  @Override
  public MMConfig updateDb(String id, Map<String, Object> data) {
    return mmRepository.update(id, data);
  }

  @Override
  protected String getRedisPrefix() {
    return REDIS_KEY;
  }

  public void getMMs() {
    symbolMMId.clear();
    SearchResult<MMConfig> result = mmRepository.findAll();
    numberOfMMs = result.getTotal();
    result.getRecords().forEach(mm -> {
      addMM(mm);
    });
  }

  private void addMM(MMConfig mmConfig) {
    Map<String, MM> idMM = symbolMMId.getOrDefault(mmConfig.getSymbol(), new HashMap<String, MM>());
    idMM.put(mmConfig.getId(), createInstance(mmConfig));
    symbolMMId.putIfAbsent(mmConfig.getSymbol(), idMM);

    List<String> ids = uidMMIds.getOrDefault(mmConfig.getUid(), new ArrayList<>());
    ids.add(mmConfig.getId());
    uidMMIds.putIfAbsent(mmConfig.getUid(), ids);
  }

  private MM createInstance(MMConfig mmConfig) {
    switch (mmConfig.getStrategy()) {
      case AVELLANEDA_STOIKOV:
        return new AvellanedaStoikov(mmConfig, appContext);
      default:
        throw new AppException(HttpMessage.INVALID_STRATEGY);
    }
  }

  @ServiceActivator(inputChannel = InternalChannel.BOOK_READY)
  public void onBookReady(String symbol) {
    if (symbolMMId.containsKey(symbol)) {
      OrderBook orderBook = gameCenter.getOrderBook(symbol);
      symbolMMId.get(symbol).values().forEach(mm -> {
        // if (mm.getMmConfig().getStatus() == MMConfigStatus.RUNNING) {
        mm.updateBestBidAsk(orderBook.getBestBid(), orderBook.getBestAsk());
        mm.updateLastPrice(orderBook.getLastPrice());
        // LOGGER.info("Run MM {} {} {}", mm.getMmConfig().getId(),
        // mm.getMmConfig().getStrategy(),
        // mm.getMmConfig().getUid());
        // mm.start(false);
        // }
      });
    }
  }

  @ServiceActivator(inputChannel = InternalChannel.DELETE_MM_USER)
  public void onDeleteMMUser(String uid) {
    List<String> ids = uidMMIds.get(uid);
    if (ids != null && !ids.isEmpty()) {
      for (String id : ids) {
        LOGGER.info("Stopping mm {}", id);
      }
      LOGGER.info("Stopped all MMs of user {}", uid);
    }
  }

  @ServiceActivator(inputChannel = InternalChannel.NEXT_FUNDING_TIME)
  public void onNextFundingTime(FundingIndex fundingIndex) {
    Map<String, MM> idMM = symbolMMId.get(fundingIndex.getSymbol());
    if (idMM != null) {
      idMM.values().forEach(mm -> {
        mm.updateNextFundingTime(fundingIndex.getNextFundingTime());
      });
    }
  }

  @ServiceActivator(inputChannel = InternalChannel.NEW_BEST_BID_PRICE)
  public void onNewBidPrice(String symbol) {
    Map<String, MM> idMM = symbolMMId.get(symbol);
    if (idMM != null) {
      OrderBook orderBook = gameCenter.getOrderBook(symbol);
      idMM.values().forEach(mm -> {
        mm.updateBestBidPrice(orderBook.getBestBid());
      });
    }
  }

  @ServiceActivator(inputChannel = InternalChannel.NEW_BEST_ASK_PRICE)
  public void onNewAskPrice(String symbol) {
    Map<String, MM> idMM = symbolMMId.get(symbol);
    if (idMM != null) {
      OrderBook orderBook = gameCenter.getOrderBook(symbol);
      idMM.values().forEach(mm -> {
        mm.updateBestAskPrice(orderBook.getBestAsk());
      });
    }
  }

  @ServiceActivator(inputChannel = InternalChannel.NEW_LAST_PRICE)
  public void onLastPrice(String symbol) {
    Map<String, MM> idMM = symbolMMId.get(symbol);
    if (idMM != null) {
      OrderBook orderBook = gameCenter.getOrderBook(symbol);
      idMM.values().forEach(mm -> {
        mm.updateLastPrice(orderBook.getLastPrice());
      });
    }
  }

  public Object create(User user, MMDto dto) throws ParseException {
    MMUser mmUser = mmUserService.getUser(dto.getMmUserKey());
    if (mmUser == null) {
      throw new AppException(HttpMessage.MM_USER_NOT_FOUND);
    }

    gameCenter.getOrderBook(dto.getSymbol());
    Strategy strategy = Strategy.valueOf(dto.getStrategy());
    Currency currency = Currency.valueOf(dto.getCurrency());
    HttpMessage validationResult = validateStrategyParams(strategy, dto);
    if (validationResult != null) {
      return HttpResponse.response(validationResult);
    }

    Long endTime = null;
    if (dto.getEndTime() != null) {
      Instant endTimeInstant = Helper.parseDateTime(dto.getEndTime()).toInstant();
      if (Instant.now().isAfter(endTimeInstant)) {
        throw new AppException(HttpMessage.TIME_PASSED);
      }
      endTime = endTimeInstant.toEpochMilli();
    }

    EndTimeStrategy endTimeStrategy = null;
    if (dto.getEndTimeStrategy() != null) {
      endTimeStrategy = EndTimeStrategy.valueOf(dto.getEndTimeStrategy());
      endTime = null;
    }

    Object params = buildParams(strategy, dto);
    MMConfig mmConfig = MMConfig.builder()
        .userKey(dto.getMmUserKey())
        .symbol(dto.getSymbol())
        .strategy(strategy)
        .uid(user.getUid())
        .endTime(endTime)
        .endTimeStrategy(endTimeStrategy)
        .nCycles(dto.getNCycles())
        .status(MMConfigStatus.IDLE)
        .initBalance(dto.getInitBalance())
        .balance(dto.getInitBalance())
        .currency(currency)
        .params(params)
        .build();
    MMConfig newMM = saveBoth(mmConfig);
    addMM(newMM);
    return newMM;
  }

  private Object buildParams(Strategy strategy, MMDto dto) {
    if (strategy == Strategy.AVELLANEDA_STOIKOV) {
      return ASParams.builder()
          .inventoryTarget(dto.getInventoryTarget())
          .inventoryRisk(dto.getInventoryRisk())
          .build();
    }

    throw new AppException(HttpMessage.INVALID_STRATEGY);
  }

  private HttpMessage validateStrategyParams(Strategy strategy, MMDto dto) {
    if (dto.getInitBalance() < 0) {
      throw new AppException(HttpMessage.INIT_BALANCE_0);
    }

    if (dto.getEndTime() == null && dto.getEndTimeStrategy() == null && dto.getNCycles() == null) {
      throw new AppException(HttpMessage.ET_REQUIRED);
    }

    if (strategy == Strategy.AVELLANEDA_STOIKOV) {
      if (dto.getInventoryTarget() == null) {
        return HttpMessage.INVENTORY_TARGET_REQUIRED;
      }
      if (dto.getInventoryRisk() == null) {
        return HttpMessage.INVENTORY_RISK_REQUIRED;
      }
      if (dto.getInventoryRisk() < 0) {
        return HttpMessage.INVENTORY_RISK_0;
      }

      return null;
    }

    return HttpMessage.INVALID_STRATEGY;
  }

  public Object duplicateConfig(User user, String id) {
    MMConfig mmConfig = getConfig(id);
    if (!mmConfig.getUid().equalsIgnoreCase(user.getUid())) {
      return HttpResponse.response(HttpMessage.NOT_FOUND);
    }

    mmConfig.setId(null);
    mmConfig.setCreatedAt(null);
    mmConfig.setUpdatedAt(null);
    mmConfig.setStatus(MMConfigStatus.IDLE);
    return saveBoth(mmConfig);
  }

  public Object getAll() {
    return symbolMMId;
  }

  public MMConfig getConfig(String id) {
    Object cache = getCache(id, MMConfig.class);
    if (cache != null) {
      return (MMConfig) cache;
    }

    MMConfig config = mmRepository.findById(id);
    if (config == null) {
      throw new AppException(HttpMessage.NOT_FOUND);
    }

    return config;
  }

  public MMConfig start(String id, boolean simulation) {
    MMConfig mmConfig = getConfig(id);
    Map<String, MM> mmId = symbolMMId.get(mmConfig.getSymbol());
    if (mmId == null || !mmId.containsKey(mmConfig.getId())) {
      throw new AppException(HttpMessage.MM_NOT_FOUND);
    }

    MM mm = mmId.get(mmConfig.getId());
    mm.start(simulation);
    return updateStatus(id, MMConfigStatus.RUNNING);
  }

  public MMConfig updateStatus(String id, MMConfigStatus staus) {
    MMConfig config = getConfig(id);
    if (staus == config.getStatus()) {
      return config;
    }

    return _updateStatus(id, staus);
  }

  public MMConfig _updateStatus(String id, MMConfigStatus staus) {
    Map<String, Object> update = new HashMap<>();
    update.put("status", staus);
    return updateBoth(id, update);
  }

}
