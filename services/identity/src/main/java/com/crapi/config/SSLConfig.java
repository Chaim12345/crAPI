package com.crapi.config;

import javax.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("server.ssl")
public class SSLConfig {
  private Boolean enabled;

  @AssertTrue
  boolean isEmabledValid() {
    return enabled != null;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(String enabled) {
    if (enabled != null) {
      if ("true".equalsIgnoreCase(enabled)
          || "yes".equalsIgnoreCase(enabled)
          || "1".equalsIgnoreCase(enabled)) {
        this.enabled = true;
        System.out.println("TLS Enabled");
      } else {
        this.enabled = false;
        System.out.println("TLS Disabled");
      }
    }
  }
}
