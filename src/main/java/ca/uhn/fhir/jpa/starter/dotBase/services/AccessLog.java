package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.jpa.starter.dotBase.entities.model.AccessLogRepository;
import ca.uhn.fhir.jpa.starter.dotBase.utils.DateUtils;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
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
}
