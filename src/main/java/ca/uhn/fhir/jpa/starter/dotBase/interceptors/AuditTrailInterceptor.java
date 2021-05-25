package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.dotBase.utils.DateUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.ExtensionUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.MetaUtils;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DateTimeType;

public class AuditTrailInterceptor {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    AuditTrailInterceptor.class
  );

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
  public void resourcePreCreate(RequestDetails theRequest, IBaseResource theResource) {
    String username = getUsername(theRequest);
    setCreationDateTime(theRequest, theResource);
    setResourceCreator(theRequest, theResource, username);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void resourcePreUpdate(
    RequestDetails theRequest,
    IBaseResource theOldResource,
    IBaseResource theNewResource
  ) {
    String username = getUsername(theRequest);
    setResourceEditor(theNewResource, username);
    theRequest.setResource(theNewResource);
  }

  private String getUsername(RequestDetails theRequestDetails) {
    if (theRequestDetails.getAttribute("_username") == null) {
      return "unknown";
    }
    return theRequestDetails.getAttribute("_username").toString();
  }

  private static void setCreationDateTime(
    RequestDetails theRequestDetails,
    IBaseResource theResource
  ) {
    DateTimeType now = new DateTimeType(DateUtils.getCurrentTimestamp());
    String system =
      "https://simplifier.net/dot.base/resource-creation-datetime-namingsystem";
    MetaUtils.setTag(
      theRequestDetails.getFhirContext(),
      theResource,
      now.getValueAsString(),
      system
    );
  }

  private static void setResourceCreator(
    RequestDetails theRequestDetails,
    IBaseResource theResource,
    String username
  ) {
    String system = "https://simplifier.net/dot.base/dotbase-username-namingsystem";
    MetaUtils.setTag(theRequestDetails.getFhirContext(), theResource, system, username);
  }

  private static void setResourceEditor(IBaseResource theResource, String username) {
    String system = "https://simplifier.net/dot.base/resource-editor-username";
    ExtensionUtils.addExtension(theResource, system, username);
  }
}
