package io.darasa.futuremm.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression(AppConfig.CONDITION_RUN_GAME)
public class MMHistoryService extends DB<MMHistory> {
  private static final String REDIS_KEY = "mm_history:";
  private static final Logger LOGGER = LoggerFactory.getLogger(MMHistoryService.class);

  @Autowired
  private MMHistoryRepository mmRepository;

  @Override
  public MMHistory saveDb(MMHistory data) {
    return mmRepository.save(data);
  }

  @Override
  public MMHistory updateDb(String id, Map<String, Object> data) {
    return mmRepository.update(id, data);
  }

  @Override
  protected String getRedisPrefix() {
    return REDIS_KEY;
  }

  public Object getById(String id, ESPaginate page) {
    return mmRepository.findByMMId(id, page);
  }

}
