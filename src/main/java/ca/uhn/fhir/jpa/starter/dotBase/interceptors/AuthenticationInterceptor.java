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
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(AuthenticationInterceptor.class);
  private static final String PROCESSING_SUB_REQUEST = "BaseHapiFhirDao.processingSubRequest";

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void preHandleIncomingRequest(RequestDetails theRequestDetails, ServletRequestDetails servletRequestDetails,
      RestOperationTypeEnum restOperationType) {
    if (theRequestDetails.getAttribute("_username") == null) {
      String username = getAuthenticatedUser(theRequestDetails);
      theRequestDetails.setAttribute("_username", username);
      setSentryUser(username);
      AccessLog.logRequest(username, theRequestDetails, restOperationType);
    }

    //TODO: ACCESS_LOG FOR SUBREQUESTS

    if (restOperationType.equals(RestOperationTypeEnum.TRANSACTION)
        && theRequestDetails.getUserData().get(PROCESSING_SUB_REQUEST) != Boolean.TRUE)
      AuditTrail.handleTransaction(theRequestDetails);
  }

  /**
   * Currently Authorization is not set on incoming requests. Thus, we retrieve
   * the username from header "X-Forwarded-User" for the moment.
   */
  private String getAuthenticatedUser(RequestDetails theRequestDetails) {
    if (theRequestDetails.getHeader("Authorization") != null) {
      Claims jwt = Authentication.verifyAndDecodeJWT(theRequestDetails);
      return getUsername(jwt);
    }
    if (theRequestDetails.getHeader("X-Forwarded-User") != null) {
      return theRequestDetails.getHeader("X-Forwarded-User");
    }
    throw new AuthenticationException("Authentication failed");
  }

  private String getUsername(Claims jwt) {
    if (!jwt.containsKey("preferred_username")) {
      throw new AuthenticationException("Authentication failed - token does not provide a username");
    }
    return jwt.get("preferred_username").toString();
  }

  private void setSentryUser(String username) {
    User user = new User();
    user.setUsername(username);
    Sentry.setUser(user);
  }
}
