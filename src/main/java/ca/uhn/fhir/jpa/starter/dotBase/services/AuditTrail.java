package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.StringType;

public class AuditTrail {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(AuditTrail.class);
  private static final Set<RestOperationTypeEnum> RESOURCE_EDIT_OPERATIONS;

  static {
    RESOURCE_EDIT_OPERATIONS = new HashSet<RestOperationTypeEnum>();
    RESOURCE_EDIT_OPERATIONS.add(RestOperationTypeEnum.CREATE);
    RESOURCE_EDIT_OPERATIONS.add(RestOperationTypeEnum.UPDATE);
    RESOURCE_EDIT_OPERATIONS.add(RestOperationTypeEnum.PATCH);
  }

  public static void logUsername(
    String username,
    RequestDetails theRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    setSentryUser(username);
    theRequestDetails.setAttribute("_username", username);
    if (RESOURCE_EDIT_OPERATIONS.contains(restOperationType))
      setResourceUserExtension(username, theRequestDetails);
  }

  private static void setResourceUserExtension(String username, RequestDetails theRequestDetails) {
    DomainResource theResource = (DomainResource) theRequestDetails.getResource();
    Extension usernameExtension = new Extension()
      .setUrl("https://simplifier.net/dot.base/resource-editor-username")
      .setValue(new StringType(username));
    theResource.getExtension().add(usernameExtension);
    theRequestDetails.setResource(theResource);
  }

  private static void setSentryUser(String username) {
    User user = new User();
    user.setUsername(username);
    Sentry.setUser(user);
  }
}
