package io.darasa.futuremm.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "strategy", ignoreInvalidFields = true, ignoreUnknownFields = true)
@Data
public class StrategyProperties {
  private String symbol;
  private String name;
}
