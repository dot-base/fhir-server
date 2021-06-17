package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.dotBase.services.AccessLog;
import ca.uhn.fhir.jpa.starter.dotBase.services.AuditTrail;
import ca.uhn.fhir.jpa.starter.dotBase.services.Authentication;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import io.jsonwebtoken.Claims;
import io.sentry.Sentry;
import io.sentry.protocol.User;

public class AuthenticationInterceptor {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    AuthenticationInterceptor.class
  );
  private static final String PROCESSING_SUB_REQUEST = "BaseHapiFhirDao.processingSubRequest";

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void preHandleIncomingRequest(
    RequestDetails theRequestDetails,
    ServletRequestDetails servletRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    String username = "unknown";
    boolean isTransaction = restOperationType.equals(RestOperationTypeEnum.TRANSACTION);
    boolean isSubRequest =
      theRequestDetails.getUserData().get(PROCESSING_SUB_REQUEST) == Boolean.TRUE;
    boolean isAuthenticated = theRequestDetails.getAttribute("_username") != null;

    if (!isAuthenticated) {
      username = getAuthenticatedUser(theRequestDetails);
      theRequestDetails.setAttribute("_username", username);
      setSentryUser(username);
      AccessLog.logRequest(username, theRequestDetails, restOperationType);
    }

    if (isTransaction && !isSubRequest) {
      AuditTrail.handleTransaction(theRequestDetails);
      AccessLog.handleTransaction(username, theRequestDetails);
    }
  }

  /**
   * Currently Authorization is not set on incoming requests. Thus, we retrieve the username from
   * header "X-Forwarded-User" for the moment.
   */
  private String getAuthenticatedUser(RequestDetails theRequestDetails) {
    if (theRequestDetails.getHeader("Authorization") != null) {
      Claims jwt = Authentication.verifyAndDecodeJWT(theRequestDetails);
      return getUsername(jwt);
    }
    String xForwardedUser = theRequestDetails.getHeader("X-Forwarded-User");
    if (xForwardedUser == null || xForwardedUser.equals("")) throw new AuthenticationException(
      "Authentication failed"
    );
    return xForwardedUser;
  }

  private String getUsername(Claims jwt) {
    if (!jwt.containsKey("preferred_username")) {
      throw new AuthenticationException(
        "Authentication failed - token does not provide a username"
      );
    }
    return jwt.get("preferred_username").toString();
  }

  private void setSentryUser(String username) {
    User user = new User();
    user.setUsername(username);
    Sentry.setUser(user);
  }
}
