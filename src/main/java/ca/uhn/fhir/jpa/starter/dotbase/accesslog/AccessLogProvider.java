package ca.uhn.fhir.jpa.starter.dotbase.accesslog;

import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.starter.dotbase.utils.BeanUtils;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.Basic;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.StringType;

public class AccessLogProvider extends JpaSystemProviderR4 {
  private static final AccessLogModel ACCESS_LOG_MODEL = (AccessLogModel) BeanUtils.getBeanByName("access_log_model");


  @Operation(name = "$logs", idempotent = true)
  public Bundle getAllLogs(
    HttpServletRequest theRequest,
    RequestDetails requestDetails,
    @OperationParam(name = "_method") StringType method,
    @OperationParam(name = "_username") StringType username,
    @OperationParam(name = "_resourcetype") StringType resourcetype,
    @OperationParam(name = "_url") StringType url,
    @OperationParam(name = "_from") StringType from,
    @OperationParam(name = "_to") StringType to,
    @OperationParam(name = "_limit") StringType limit
  )
    throws Exception {
    Map<String, StringType> params = null;
    if (requestDetails.getParameters().size() > 0) {
      params = new HashMap<>();
      params.put("method", method);
      params.put("username", username);
      params.put("resourcetype", resourcetype);
      params.put("url", url);
      params.put("from", from);
      params.put("to", to);
    }
    if(limit == null)
    limit = new StringType("1000");
    List<AccessLog> logs = ACCESS_LOG_MODEL.getLogs(params, limit);
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

  private StringType logToStringType(AccessLog log) {
    String concat =
      "method: " +
      log.method +
      ", username: " +
      log.username +
      ", url: " +
      log.url +
      ", timestamp: " +
      log.timestamp;
    return new StringType(concat);
  }
}
