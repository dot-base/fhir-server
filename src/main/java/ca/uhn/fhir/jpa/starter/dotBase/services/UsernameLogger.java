package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import io.jsonwebtoken.Claims;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Meta;

public class UsernameLogger {
  private static final org.slf4j.Logger OUR_LOG = org.slf4j.LoggerFactory.getLogger(
    UsernameLogger.class
  );
  private static final Set<RestOperationTypeEnum> RESOURCE_OPERATIONS;

  static {
    RESOURCE_OPERATIONS = new HashSet<RestOperationTypeEnum>();
    RESOURCE_OPERATIONS.add(RestOperationTypeEnum.CREATE);
    RESOURCE_OPERATIONS.add(RestOperationTypeEnum.UPDATE);
    RESOURCE_OPERATIONS.add(RestOperationTypeEnum.PATCH);
  }

  public static void log(
    Claims jwt,
    RequestDetails theRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    String username = getUsername(jwt);
    logUsername(username, theRequestDetails, restOperationType);
  }

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
    Meta meta = theResource.getMeta();
    List<Coding> tags = meta.getTag();
    Coding code = new Coding()
    .setSystem("https://simplifier.net/dot.base/requesting-username-namingsystem");
    code.setCode(username);
    tags.add(code);
    theResource.setMeta(meta.setTag(tags));
    theRequestDetails.setResource(theResource);
  }

  private static void setSentryUser(String username) {
    User user = new User();
    user.setUsername(username);
    Sentry.setUser(user);
  }
}
