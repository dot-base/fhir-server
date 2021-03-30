package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.jpa.starter.dotBase.entities.model.AccessLogRepository;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.text.SimpleDateFormat;
import java.util.Date;
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
      restOperationType.toString(),
      username,
      theRequestDetails.getCompleteUrl(),
      theRequestDetails.getResourceName(),
      getCurrentTimestamp()
    );
  }

  private static String getCurrentTimestamp() {
    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    return formatter.format(new Date());
  }
}
