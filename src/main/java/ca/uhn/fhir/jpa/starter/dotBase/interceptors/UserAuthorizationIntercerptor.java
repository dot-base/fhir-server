package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import java.util.List;
import ca.uhn.fhir.jpa.starter.dotBase.DotbaseProperties;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.auth.AuthorizationInterceptor;
import ca.uhn.fhir.rest.server.interceptor.auth.IAuthRule;
import ca.uhn.fhir.rest.server.interceptor.auth.RuleBuilder;

/**
 * Determines whether the caller is admin, normal user or no user
 * and authorizes accordingly.
 * 
 * @pre register after {@link AuthenticationInterceptor}
 */
public class UserAuthorizationIntercerptor extends AuthorizationInterceptor {
    private static final List<IAuthRule> ADMIN_RULES = new RuleBuilder()
            .allowAll().build();
    private static final List<IAuthRule> USER_RULES = new RuleBuilder()
            .deny("Log acess forbidden for non-admin")
                .operation()
                .named("$logs")
                .atAnyLevel()
                .andAllowAllResponses()
                .andThen()
            .deny("Force-reindexing forbidden for non-admin")
                .operation()
                .named("$mark-all-resources-for-reindexing")
                .atAnyLevel()
                .andAllowAllResponses()
                .andThen()
            .deny("Deleting meta (profiles, tags, security labels) forbidden for non-admin")
                .operation()
                .named("$meta-delete")
                .atAnyLevel()
                .andAllowAllResponses()
                .andThen()
            .allowAll().build();
    private static final List<IAuthRule> DENY_ALL_RULES = new RuleBuilder()
            .denyAll().build();
    
    private String getUsername(RequestDetails theRequestDetails) {
        return theRequestDetails.getAttribute("_username").toString();
    }

    private boolean isUserAdmin(String username) {
        return username == DotbaseProperties.get("Dotbase.AdminUserName");
    }

    private boolean isUser(String username) {
        return username != null;
    }

    @Override
    public List<IAuthRule> buildRuleList(RequestDetails theRequestDetails) {
        String username = getUsername(theRequestDetails);

        if (isUserAdmin(username)) return ADMIN_RULES;
        if (isUser(username)) return USER_RULES;
        return DENY_ALL_RULES;
    }

}
