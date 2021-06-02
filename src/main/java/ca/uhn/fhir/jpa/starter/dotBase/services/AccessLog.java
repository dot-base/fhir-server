package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.jpa.starter.dotBase.entities.model.AccessLogRepository;
import ca.uhn.fhir.jpa.starter.dotBase.utils.DateUtils;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.springframework.web.context.ContextLoaderListener;

public class AccessLog {
  private static final AccessLogRepository ACCESS_LOG_REPOSITORY = ContextLoaderListener
    .getCurrentWebApplicationContext()
    .getBean("access_log_repository", AccessLogRepository.class);

  public static void logRequest(
    String username,
    RequestDetails theRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    ACCESS_LOG_REPOSITORY.createLog(
      theRequestDetails.getRequestId(),
      restOperationType.toString(),
      username,
      theRequestDetails.getCompleteUrl(),
      theRequestDetails.getResourceName(),
      DateUtils.getCurrentTimestamp()
    );
  }

  public static void handleTransaction(String username, RequestDetails theRequest) {
    if (theRequest.getResource() instanceof Bundle) {
      Bundle theResource = (Bundle) theRequest.getResource();
      List<BundleEntryComponent> entries = theResource.getEntry();
      entries.forEach(entry -> transactionEntry(username, theRequest, entry));
    }
  }

  private static void transactionEntry(
    String username,
    RequestDetails theRequest,
    BundleEntryComponent entry
  ) {
    logSubRequest(
      theRequest.getRequestId(),
      entry.getRequest().getMethod().toCode(),
      username,
      entry.getRequest().getUrl(),
      entry.getResource().getResourceType().name()
    );
  }

  public static void logSubRequest(
    String requestId,
    String method,
    String username,
    String url,
    String resourceType
  ) {
    ACCESS_LOG_REPOSITORY.createLog(
      requestId,
      method,
      username,
      url,
      resourceType,
      DateUtils.getCurrentTimestamp()
    );
  }
}
