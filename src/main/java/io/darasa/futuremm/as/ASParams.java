package io.darasa.futuremm.as;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ASParams {
  @JsonProperty("inventory_target")
  private double inventoryTarget;

  @JsonProperty("inventory_risk")
  private double inventoryRisk;
}
