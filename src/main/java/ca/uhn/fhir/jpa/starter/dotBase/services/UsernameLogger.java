package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import io.jsonwebtoken.Claims;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.StringType;

public class UsernameLogger {
  private static final org.slf4j.Logger OUR_LOG = org.slf4j.LoggerFactory.getLogger(
    UsernameLogger.class
  );
  private static final Set<RestOperationTypeEnum> RESOURCE_OPERATIONS;

  static {
    RESOURCE_OPERATIONS = new HashSet<RestOperationTypeEnum>();
    RESOURCE_OPERATIONS.add(RestOperationTypeEnum.CREATE);
    RESOURCE_OPERATIONS.add(RestOperationTypeEnum.UPDATE);
  }

  public static void log(
    Claims jwt,
    RequestDetails theRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    String username = getUsername(jwt);
    logUsername(username, theRequestDetails, restOperationType);
  }

  // TBD: Allow unknown users or throw Exception here?
  private static String getUsername(Claims jwt) {
    if (!jwt.containsKey("preferred_username")) return "unknown";
    return jwt.get("preferred_username").toString();
  }

  private static void logUsername(
    String username,
    RequestDetails theRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    setSentryUser(username);
    theRequestDetails.setAttribute("_username", username);
    if (RESOURCE_OPERATIONS.contains(restOperationType)) setResourceUserExtension(
      username,
      theRequestDetails
    );
  }

  private static void setResourceUserExtension(
    String username,
    RequestDetails theRequestDetails
  ) {
    DomainResource theResource = (DomainResource) theRequestDetails.getResource();
    Extension userExtension = new Extension();
    userExtension.setUrl("https://simplifier.net/dot.base/requesting-username");
    userExtension.setValue(new StringType().setValue(username));
    theResource.addExtension(userExtension);
    theRequestDetails.setResource(theResource);
  }

  private static void setSentryUser(String username) {
    User user = new User();
    user.setUsername(username);
    Sentry.setUser(user);
  }
}
