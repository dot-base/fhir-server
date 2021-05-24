package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.dotBase.services.AccessLog;
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

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void preHandleIncomingRequest(
    RequestDetails theRequestDetails,
    ServletRequestDetails servletRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    /**
     * Currently Authorization is not set on incoming requests. Thus, we retrieve
     * the username from header "X-Forwarded-User" for the moment.
     */
    if (theRequestDetails.getHeader("Authorization") != null) {
      Claims jwt = Authentication.verifyAndDecodeJWT(theRequestDetails);
      String username = getUsername(jwt);
      theRequestDetails.setAttribute("_username", username);
      setSentryUser(username);
      AccessLog.logRequest(username, theRequestDetails, restOperationType);
      return;
    }
    if (theRequestDetails.getHeader("X-Forwarded-User") != null) {
      String username = theRequestDetails.getHeader("X-Forwarded-User");
      theRequestDetails.setAttribute("_username", username);
      setSentryUser(username);
      AccessLog.logRequest(username, theRequestDetails, restOperationType);
      return;
    }
    throw new AuthenticationException("Authentication failed");
  }

  private static String getUsername(Claims jwt) {
    if (!jwt.containsKey("preferred_username")) {
      throw new AuthenticationException(
        "Authentication failed - token does not provide a username"
      );
    }
    return jwt.get("preferred_username").toString();
  }

  private static void setSentryUser(String username) {
    User user = new User();
    user.setUsername(username);
    Sentry.setUser(user);
  }
}
