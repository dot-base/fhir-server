package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import io.sentry.Sentry;
import io.sentry.protocol.User;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.xml.bind.DatatypeConverter;

public class IncomingRequestInterceptor {

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void preHandleIncomingRequest(
    RequestDetails theRequestDetails,
    ServletRequestDetails servletRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    String credentials = "";
    String authHeader = theRequestDetails.getHeader("Authorization");
    if (authHeader != null && authHeader.toLowerCase().startsWith("bearer")) {
      String authToken = authHeader.toLowerCase().substring("bearer".length()).trim();
      byte[] tokenDecoded = Base64.getDecoder().decode(authToken);
      credentials = new String(tokenDecoded, StandardCharsets.UTF_8);
    }
    User user = new User();
    user.setId(credentials);
    Sentry.setUser(user);
    theRequestDetails.setAttribute("_userDetails", credentials);
  }
}
