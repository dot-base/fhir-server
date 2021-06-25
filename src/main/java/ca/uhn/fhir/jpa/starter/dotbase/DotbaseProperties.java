package ca.uhn.fhir.jpa.starter.dotbase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "dotbase")
@Configuration
@EnableConfigurationProperties
public class DotbaseProperties {

  private ResourceUrls resource_urls = null;

  private String admin_username;
  private Boolean authentication_interceptor_enabled = true;
  private Boolean error_monitoring_enabled = true;
  private String identity_provider_realm;
  private String realm_public_key;
  private Boolean resolve_external_references = true;
  private String server_name= "dotbase FHIR Server";

  public String getAdminUsername() {
    return admin_username;
  }

  public void setAdminUsername(String admin_username) {
    this.admin_username = admin_username;
  }


  public ResourceUrls getResourceUrls() {
    return resource_urls;
  }

  public void setResourceUrls(ResourceUrls resource_urls) {
    this.resource_urls = resource_urls;
  }

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

  public static class ResourceUrls {
    public static String namingsystem_dotbase_username= "https://dotbase.org/fhir/NamingSystem/dotbase-username";
    public static String namingsystem_creation_datetime= "https://dotbase.org/fhir/NamingSystem/creation-datetime";
    public static String structuredefinition_resource_editor= "https://dotbase.org/fhir/StructureDefinition/resource-editor"; 
    public static String structuredefinition_document_draft_action= "https://dotbase.org/fhir/StructureDefinition/document-draft-action";  

  }
}
