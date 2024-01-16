package io.darasa.futuremm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MMDto {
  @NotEmpty
  private String mmUserKey;

  @NotEmpty
  private String symbol;

  @EnumValidator(clazz = Strategy.class)
  private String strategy;

  @EnumValidator(clazz = Currency.class)
  private String currency;

  @Positive
  private double initBalance;
  private String endTime;

  @EnumValidator(clazz = EndTimeStrategy.class, nullable = true)
  private String endTimeStrategy;

  @JsonProperty("nCycles")
  @Positive
  private Long nCycles;

  private Double inventoryTarget;
  private Double inventoryRisk;
  private Double marketVolatility;

}
