package ca.uhn.fhir.jpa.starter.dotBase.entities;

import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.starter.dotBase.entities.entity.AccessLog;
import ca.uhn.fhir.jpa.starter.dotBase.entities.model.AccessLogRepository;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.annotation.RawParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.BundleProviders;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.web.context.ContextLoaderListener;

public class AccessLogProvider extends JpaSystemProviderR4 {
  private static final AccessLogRepository ACCESS_LOG_REPOSITORY = ContextLoaderListener
      .getCurrentWebApplicationContext().getBean("access_log_repository", AccessLogRepository.class);

  @Operation(name = "$logs", idempotent = true)
  public Bundle getAllLogs(HttpServletRequest theRequest, RequestDetails requestDetails,
      @OperationParam(name = "_type") StringType resourceType, @OperationParam(name = "_username") StringType username,
      @OperationParam(name = "_resourcetype") StringType resourcetype, @OperationParam(name = "_from") DateType from,
      @OperationParam(name = "_to") DateType to) throws Exception {
    // TODO: Allow searching accesLog with queryParams instead of findall only
    List<AccessLog> logs = ACCESS_LOG_REPOSITORY.findAll();
    return responseBundle(logs);
  }

  private Bundle responseBundle(List<AccessLog> logs) {
    Bundle res = new Bundle();
    for (AccessLog log : logs) {
      res.addEntry(getBundleEntry(log));
    }
    return res;
  }

  private BundleEntryComponent getBundleEntry(AccessLog log) {
    Basic entry = new Basic();
    BundleEntryComponent entryComponent = new BundleEntryComponent();
    entry.addExtension("accessLog/" + log.id, logToStringType(log));
    return entryComponent.setResource(entry);
  }

  //TODO: format String representation
  private StringType logToStringType(AccessLog log) {
    String concat = "method: " + log.method + ", username: " + log.username + ", url: " + log.url + ", timestamp: "
        + log.timestamp;
    return new StringType(concat);
  }
}
