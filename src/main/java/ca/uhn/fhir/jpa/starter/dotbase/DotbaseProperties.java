package ca.uhn.fhir.jpa.starter.dotbase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "dotbase")
@Configuration
@EnableConfigurationProperties
public class DotbaseProperties {
  private Boolean error_monitoring_enabled = true;
  private String server_name= "dotbase FHIR Server";

  public Boolean getError_monitoring_enabled() {
    return error_monitoring_enabled;
  }

  public void setError_monitoring_enabled(Boolean error_monitoring_enabled) {
    this.error_monitoring_enabled = error_monitoring_enabled;
  }

  public String getServer_name() {
    return server_name;
  }

  public void setServer_name(String server_name) {
    this.server_name = server_name;
  }

}
