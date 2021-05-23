package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class AuditTrailInterceptor {
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(AuditTrailInterceptor.class);

    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
    public void resourcePreCreate(RequestDetails theRequest, IBaseResource theResource) {
        ourLog.debug("Validating address on for create {}, {}");
    }

    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
    public void resourcePreUpdate(RequestDetails theRequest, IBaseResource theOldResource,
            IBaseResource theNewResource) {
        ourLog.debug("Validating address on for update {}, {}, {}");
    }
}
