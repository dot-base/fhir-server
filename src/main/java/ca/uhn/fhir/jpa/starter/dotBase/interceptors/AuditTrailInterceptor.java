package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DateTimeType;

import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.dotBase.utils.DateUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.ExtensionUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.MetaUtils;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.rest.api.server.RequestDetails;

public class AuditTrailInterceptor {
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(AuditTrailInterceptor.class);

    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_CREATED)
    public void resourcePreCreate(RequestDetails theRequest, IBaseResource theResource) {
        if (hasUsername(theRequest)) {
            String username = theRequest.getAttribute("_username").toString();
            setCreationDateTime(theRequest);
            setResourceCreator(username, theRequest);
        }
    }

    @Hook(Pointcut.STORAGE_PRESTORAGE_RESOURCE_UPDATED)
    public void resourcePreUpdate(RequestDetails theRequest, IBaseResource theOldResource,
            IBaseResource theNewResource) {
        if (hasUsername(theRequest)) {
            String username = theRequest.getAttribute("_username").toString();
            setResourceEditor(username, theRequest);
        }
    }

    private boolean hasUsername(RequestDetails theRequestDetails) {
        return theRequestDetails.getAttribute("_username") == null;
    }

    private static void setCreationDateTime(RequestDetails theRequestDetails) {
        DateTimeType now = new DateTimeType(DateUtils.getCurrentTimestamp());
        String system = "https://simplifier.net/dot.base/resource-creation-datetime-namingsystem";
        MetaUtils.setTag(theRequestDetails, now.getValueAsString(), system);
    }

    private static void setResourceCreator(String username, RequestDetails theRequestDetails) {
        String system = "https://simplifier.net/dot.base/dotbase-username-namingsystem";
        MetaUtils.setTag(theRequestDetails, username, system);
    }

    private static void setResourceEditor(String username, RequestDetails theRequestDetails) {
        String system = "https://simplifier.net/dot.base/resource-editor-username";
        ExtensionUtils.addExtension(theRequestDetails, system, username);
    }
}
