package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import org.hl7.fhir.instance.model.api.IBaseResource;
import ca.uhn.fhir.jpa.starter.dotBase.services.AuditTrail;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class AuditTrailInterceptor {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(AuditTrailInterceptor.class);

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
  public void resourcePreCreate(RequestDetails theRequest, IBaseResource theResource) {

    String username = AuditTrail.getUsername(theRequest);
    AuditTrail.setCreationDateTime(theRequest, theResource);
    AuditTrail.setResourceCreator(theRequest, theResource, username);
  }

  @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
  public void resourcePreUpdate(RequestDetails theRequest, IBaseResource theOldResource, IBaseResource theNewResource) {
    String username = AuditTrail.getUsername(theRequest);
    AuditTrail.setResourceEditor(theNewResource, username);
    theRequest.setResource(theNewResource);
  }

}
