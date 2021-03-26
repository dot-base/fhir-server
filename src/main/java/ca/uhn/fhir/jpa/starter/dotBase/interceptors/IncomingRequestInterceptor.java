package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.dotBase.services.Authentication;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import io.jsonwebtoken.Claims;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.StringType;

public class IncomingRequestInterceptor {
  private static final org.slf4j.Logger OUR_LOG = org.slf4j.LoggerFactory.getLogger(
    IncomingRequestInterceptor.class
  );

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void preHandleIncomingRequest(
    RequestDetails theRequestDetails,
    ServletRequestDetails servletRequestDetails,
    RestOperationTypeEnum restOperationType
  ) {
    Claims jwt;

    jwt = Authentication.verifyAndDecodeJWT(theRequestDetails);
    // IncomingRequestInterceptor.setSentryUserDetails(jwt);
    theRequestDetails.setAttribute("_userDetails", jwt.getId());
    if (
      restOperationType.equals(RestOperationTypeEnum.CREATE) ||
      restOperationType.equals(RestOperationTypeEnum.UPDATE)
    ) {
      setResourceUserDetails(jwt, theRequestDetails);
    }
  }

  private static void setResourceUserDetails(
    Claims jwt,
    RequestDetails theRequestDetails
  ) {
    DomainResource theResource = (DomainResource) theRequestDetails.getResource();
    Meta meta = theResource.getMeta();
    Extension userExtension = new Extension();
    userExtension.setUrl("userDetailsExtension");
    userExtension.setValue(new StringType().setValue("theValue"));
    meta.addExtension(userExtension);
    theResource.setMeta(meta);
    theRequestDetails.setResource(theResource);
  }

  private static void setSentryUserDetails(String jwt) {
    User user = new User();
    user.setId(jwt);
    Sentry.setUser(user);
  }
}
