package ca.uhn.fhir.jpa.starter.dotbase.interceptor;

import ca.uhn.fhir.jpa.starter.AppProperties;
import ca.uhn.fhir.jpa.starter.dotbase.DotbaseProperties;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Determines the requesters userrole (admin or normal user) and authorizes
 * access accordingly.
 */
@Component
public class UserRoleInterceptor extends AuthorizationInterceptor {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(UserRoleInterceptor.class);

  private static final String[] RESTRICTED_ENDPOINTS = { "$logs", "$mark-all-resources-for-reindexing",
      "$meta-delete" };

  private static final List<IAuthRule> ADMIN_RULES = new RuleBuilder().allowAll().build();
  private static final List<IAuthRule> USER_RULES = buildUserRules();

  private static List<IAuthRule> buildUserRules() {
    RuleBuilder ruleBuilder = new RuleBuilder();
    for (String endpoint : RESTRICTED_ENDPOINTS)
      ruleBuilder.deny("Restricted Endpoint or Operation - " + endpoint).operation().named("$logs").atAnyLevel()
          .andAllowAllResponses().andThen();
    return ruleBuilder.allowAll().build();
  }

  @Autowired
  DotbaseProperties dotbaseProperties;

  private String getUsername(RequestDetails theRequestDetails) {
    String username = theRequestDetails.getAttribute("_username").toString();
    if (username == null) {
      throw new AuthenticationException("Authentication failed.");
    }
    return username;
  }

  private boolean isAdmin(String username) {
    String adminUserName = dotbaseProperties.getAdminUsername();
    if (adminUserName == null || adminUserName.equals("")) {
      ourLog.warn("Missing property admin username.");
    }
    return username.equals(adminUserName);
  }

  private boolean isUser(String username) {
    return username != null;
  }

  @Override
  public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
    String username = getUsername(theRequestDetails);
    if (isAdmin(username))
      return ADMIN_RULES;
    if (isUser(username))
      return USER_RULES;
    return ADMIN_RULES;
  }
}