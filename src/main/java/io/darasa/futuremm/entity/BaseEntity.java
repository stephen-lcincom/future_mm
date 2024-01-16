package io.darasa.futuremm.entity;

import java.io.Serializable;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.darasa.futuremm.constaint.ColName;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public abstract class BaseEntity implements Serializable {
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty(ColName.ID)
  private String id;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty(ColName.CREATED_AT)
  private Timestamp createdAt;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("updated_at")
  private Timestamp updatedAt;
}
