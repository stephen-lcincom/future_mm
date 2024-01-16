package io.darasa.futuremm.entity;

import java.util.Currency;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.darasa.annotation.TableName;
import io.darasa.futuremm.as.EndTimeStrategy;
import io.darasa.futuremm.enums.Strategy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TableName(name = "mm_config")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class MMConfig extends BaseEntity {
  @JsonProperty(value = "user_key")
  private String userKey;
  private String symbol;
  private Strategy strategy;
  private String uid;

  @JsonProperty(value = "init_balance")
  private double initBalance;

  private double balance;
  private Currency currency;
  private MMConfigStatus status;

  @JsonProperty(value = "end_time")
  private Long endTime;

  @JsonProperty(value = "end_time_strategy")
  private EndTimeStrategy endTimeStrategy;

  @JsonProperty(value = "n_cycles")
  private Long nCycles;

  @JsonSerialize(converter = Object2String.class)
  @JsonDeserialize(converter = String2Object.class)
  private Object params;
}
