package ca.uhn.fhir.jpa.starter.dotbase.services;

import ca.uhn.fhir.jpa.starter.dotbase.accesslog.AccessLogModel;
import ca.uhn.fhir.jpa.starter.dotbase.utils.BeanUtils;
import ca.uhn.fhir.jpa.starter.dotbase.utils.DateUtils;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.List;

import org.hl7.fhir.dstu2016may.model.Enumerations.ResourceType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;

public class AccessLog {
  private static final AccessLogModel ACCESS_LOG_MODEL = (AccessLogModel) BeanUtils.getBeanByName("access_log_model");

  public static void logRequest(String username, RequestDetails theRequestDetails,
      RestOperationTypeEnum restOperationType) {
    ACCESS_LOG_MODEL.createLog(theRequestDetails.getRequestId(), restOperationType.toString(), username,
        theRequestDetails.getCompleteUrl(), theRequestDetails.getResourceName(), DateUtils.getCurrentTimestamp());
  }

  public static void handleTransaction(String username, RequestDetails theRequest) {
    if (!(theRequest.getResource() instanceof Bundle)) {
      return;
    }
    Bundle theResource = (Bundle) theRequest.getResource();
    List<BundleEntryComponent> entries = theResource.getEntry();
    entries.forEach(entry -> transactionEntry(username, theRequest, entry));
  }

  private static void transactionEntry(String username, RequestDetails theRequest, BundleEntryComponent entry) {
    logSubRequest(theRequest.getRequestId(), entry.getRequest().getMethod().toCode(), username,
        entry.getRequest().getUrl(), getResourceType(entry));
  }

  private static String getResourceType(BundleEntryComponent entry) {
    if (entry.getResource() != null) {
      return entry.getResource().getResourceType().name();
    }
    String resourceType = entry.getRequest().getUrl().split("/")[0];
    for (ResourceType currentResourceType : ResourceType.values()) {
      String resourceTypeString = currentResourceType.toString();
      if (resourceType.toUpperCase().equals(resourceTypeString)) {
        return resourceType;
      }
    }
    return null;
  }

  public static void logSubRequest(String requestId, String method, String username, String url, String resourceType) {
    ACCESS_LOG_MODEL.createLog(requestId, method, username, url, resourceType, DateUtils.getCurrentTimestamp());
  }
}
