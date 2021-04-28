package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.jpa.starter.dotBase.Utils;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.StringType;

public class AuditTrail {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(AuditTrail.class);
  private static final Set<RestOperationTypeEnum> RESOURCE_EDITING_OPERATIONS;

  static {
    RESOURCE_EDITING_OPERATIONS = new HashSet<RestOperationTypeEnum>();
    RESOURCE_EDITING_OPERATIONS.add(RestOperationTypeEnum.CREATE);
    RESOURCE_EDITING_OPERATIONS.add(RestOperationTypeEnum.UPDATE);
    RESOURCE_EDITING_OPERATIONS.add(RestOperationTypeEnum.PATCH);
  }

  public static void logRequest(
    String username,
    RequestDetails theRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    setSentryUser(username);
    theRequestDetails.setAttribute("_username", username);

    if (restOperationType.equals(RestOperationTypeEnum.CREATE)){
        setResourceCreator( username, theRequestDetails );
          setCreationDateTime(theRequestDetails);
    }

    if (RESOURCE_EDITING_OPERATIONS.contains(restOperationType))
         setResourceEditor(username, theRequestDetails );
  }

  private static void setResourceCreator(
    String username,
    RequestDetails theRequestDetails
  ) {
    Coding usernameCoding = new Coding()
      .setSystem("https://simplifier.net/dot.base/dotbase-username-namingsystem")
      .setCode(username);

    DomainResource theResource = (DomainResource) theRequestDetails.getResource();
    Meta meta = theResource.getMeta();
    List<Coding> tags = meta.getTag();
    tags.add(usernameCoding);
    theResource.setMeta(meta.setTag(tags));
    theRequestDetails.setResource(theResource);
  }

  private static void setResourceEditor(
    String username,
    RequestDetails theRequestDetails
  ) {
    Extension usernameExtension = new Extension()
      .setUrl("https://simplifier.net/dot.base/resource-editor-username")
      .setValue(new StringType(username));

    DomainResource theResource = (DomainResource) theRequestDetails.getResource();
    theResource.getExtension().add(usernameExtension);
    theRequestDetails.setResource(theResource);
  }

  private static void setSentryUser(String username) {
    User user = new User();
    user.setUsername(username);
    Sentry.setUser(user);
  }

  private static void setCreationDateTime(RequestDetails theRequestDetails) {
    DateTimeType now = new DateTimeType(Utils.getCurrentTimestamp());
    Coding creationDate = new Coding()
    .setSystem("https://simplifier.net/dot.base/resource-creation-datetime-namingsystem")
    .setCode(now.getValueAsString());

    DomainResource theResource = (DomainResource) theRequestDetails.getResource();
    Meta meta = theResource.getMeta();
    List<Coding> tags = meta.getTag();
    tags.add(creationDate);
    theResource.setMeta(meta.setTag(tags));
    theRequestDetails.setResource(theResource);
  }
}
