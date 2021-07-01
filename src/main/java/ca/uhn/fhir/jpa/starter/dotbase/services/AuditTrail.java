package ca.uhn.fhir.jpa.starter.dotbase.services;

import ca.uhn.fhir.jpa.starter.dotbase.DotbaseProperties.ResourceUrls;
import ca.uhn.fhir.jpa.starter.dotbase.utils.DateUtils;
import ca.uhn.fhir.jpa.starter.dotbase.utils.ExtensionUtils;
import ca.uhn.fhir.jpa.starter.dotbase.utils.MetaUtils;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditTrail {
  private static final Logger ourLog = LoggerFactory.getLogger(AuditTrail.class);

  public static void setCreationDateTime(RequestDetails theRequest, IBaseResource theResource) {
    DateTimeType now = new DateTimeType(DateUtils.getCurrentTimestamp());
    String system = ResourceUrls.namingsystem_creation_datetime;
    MetaUtils.setTag(theRequest.getFhirContext(), theResource, system, now.getValueAsString());
  }

  public static void setResourceCreator(RequestDetails theRequest, IBaseResource theResource) {
    String username = getUsername(theRequest);
    String system = ResourceUrls.namingsystem_dotbase_username;
    MetaUtils.setTag(theRequest.getFhirContext(), theResource, system, username);
  }

  public static void setResourceEditor(RequestDetails theRequest, IBaseResource theResource) {
    String username = getUsername(theRequest);
    String system = ResourceUrls.structuredefinition_resource_editor;
    ExtensionUtils.addExtension(theResource, system, new CodeType(username));
  }

  private static String getUsername(RequestDetails theRequest) {
    if (theRequest.getAttribute("_username") == null) {
      return "unknown";
    }
    return theRequest.getAttribute("_username").toString();
  }
}