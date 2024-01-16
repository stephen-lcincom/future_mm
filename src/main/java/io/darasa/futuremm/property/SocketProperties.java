package io.darasa.futuremm.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "socket")
@Data
public class SocketProperties {
  private String baseUrl;
  private String token;

  public String getUrl() {
    return String.format("%s?token=%s", baseUrl, token);
  }
}
