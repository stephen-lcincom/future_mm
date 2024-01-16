package io.darasa.futuremm.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity(name = "mm_history")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MMHistory extends BaseEntity {
  @JsonProperty(ColName.MMHistory.MM_ID)
  private String mmId;

  @JsonSerialize(converter = Object2String.class)
  @JsonDeserialize(converter = String2Object.class)
  private Object data;
}
