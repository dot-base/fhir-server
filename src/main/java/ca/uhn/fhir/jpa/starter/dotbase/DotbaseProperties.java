package ca.uhn.fhir.jpa.starter.dotbase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "dotbase")
@Configuration
@EnableConfigurationProperties
public class DotbaseProperties {

  private Boolean authentication_interceptor_enabled = true;
  private Boolean error_monitoring_enabled = true;
  private String identity_provider_realm = "";
  private String realm_public_key = "";
  private Boolean resolve_external_references = true;
  private String server_name= "dotbase FHIR Server";

  public Boolean  getAuthenticationInterceptorEnabled() {
    return authentication_interceptor_enabled;
  }

  public void  setAuthenticationInterceptorEnabled(Boolean authentication_interceptor_enabled) {
    this.authentication_interceptor_enabled = authentication_interceptor_enabled;
  }

  public Boolean getError_monitoring_enabled() {
    return error_monitoring_enabled;
  }

  public void setError_monitoring_enabled(Boolean error_monitoring_enabled) {
    this.error_monitoring_enabled = error_monitoring_enabled;
  }
  public String getIdentityProviderRealm() {
    return identity_provider_realm;
  }

  public void setIdentityProviderRealm(String identity_provider_realm) {
    this.identity_provider_realm = identity_provider_realm;
  }

  public String getRealmPublicKey() {
    return realm_public_key;
  }

  public void setRealmPublicKey(String realm_public_key) {
    this.realm_public_key = realm_public_key;
  }

  public Boolean getResolveExternalReferences() {
    return resolve_external_references;
  }

  public void setResolveExternalReferences(Boolean resolve_external_references) {
    this.resolve_external_references = resolve_external_references;
  }
  
  public String getServer_name() {
    return server_name;
  }

  public void setServer_name(String server_name) {
    this.server_name = server_name;
  }
}
