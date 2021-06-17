package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.jpa.starter.dotBase.DotbaseProperties;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import java.util.List;

/**
 * Determines whether the caller is admin, normal user or no user and authorizes
 * access accordingly.
 */
public class UserAuthorizationInterceptor extends AuthorizationInterceptor {
  private static final String[] RESTRICTED_ENDPOINTS = {
    "$logs",
    "$mark-all-resources-for-reindexing",
    "$meta-delete"
  };

  private static final List<IAuthRule> ADMIN_RULES = new RuleBuilder().allowAll().build();
  private static final List<IAuthRule> DENY_ALL_RULES = new RuleBuilder().denyAll().build();
  private static final List<IAuthRule> USER_RULES = buildUserRules();

  private static List<IAuthRule> buildUserRules() {
    RuleBuilder ruleBuilder = new RuleBuilder();
    for (String endpoint : RESTRICTED_ENDPOINTS) ruleBuilder
      .deny("Restricted Endpoint or Operation - " + endpoint)
      .operation()
      .named("$logs")
      .atAnyLevel()
      .andAllowAllResponses()
      .andThen();
    return ruleBuilder.allowAll().build();
  }

  private String getUsername(RequestDetails theRequestDetails) {
    String username = theRequestDetails.getAttribute("_username").toString();
    if (username == null) throw new Error("Missing username");
    return username;
  }

  private boolean isAdmin(String username) {
    String adminUserName = DotbaseProperties.get("Dotbase.AdminUserName");
    if (adminUserName == null || adminUserName.equals("")) System.out.println(
      "WARN: No admin user set in dotbase.properties"
    );
    return username.equals(adminUserName);
  }

  private boolean isUser(String username) {
    return username != null;
  }

  @Override
  public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
    if (!HapiProperties.isAuthenticationInterceptorEnabled()) return ADMIN_RULES;

    String username = getUsername(theRequestDetails);
    if (isAdmin(username)) return ADMIN_RULES;
    if (isUser(username)) return USER_RULES;
    return DENY_ALL_RULES;
  }
}
