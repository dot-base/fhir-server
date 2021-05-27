package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.starter.dotBase.PlainSystemProviderR4;
import ca.uhn.fhir.jpa.starter.dotBase.interceptors.AuditTrailInterceptor;
import ca.uhn.fhir.jpa.starter.dotBase.utils.DateUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.ExtensionUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.MetaUtils;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

public class AuditTrail {
    public static String getUsername(RequestDetails theRequestDetails) {
        if (theRequestDetails.getAttribute("_username") == null) {
            return "unknown";
        }
        return theRequestDetails.getAttribute("_username").toString();
    }

    public static void setCreationDateTime(RequestDetails theRequestDetails, IBaseResource theResource) {
        DateTimeType now = new DateTimeType(DateUtils.getCurrentTimestamp());
        String system = "https://simplifier.net/dot.base/resource-creation-datetime-namingsystem";
        MetaUtils.setTag(theRequestDetails.getFhirContext(), theResource, now.getValueAsString(), system);
    }

    public static void setResourceCreator(RequestDetails theRequestDetails, IBaseResource theResource,
            String username) {
        String system = "https://simplifier.net/dot.base/dotbase-username-namingsystem";
        MetaUtils.setTag(theRequestDetails.getFhirContext(), theResource, system, username);
    }

    public static void setResourceEditor(IBaseResource theResource, String username) {
        String system = "https://simplifier.net/dot.base/resource-editor-username";
        ExtensionUtils.addExtension(theResource, system, username);
    }

    public static void handleTransaction(RequestDetails theRequestDetails) {
        if (theRequestDetails.getResource() instanceof Bundle) {
            Bundle theResource = (Bundle) theRequestDetails.getResource();
            List<BundleEntryComponent> entries = theResource.getEntry();
            entries.forEach(entry -> transactionEntry(theRequestDetails, entry));
        }
    }

    // TODO: sort by version
    private static void transactionEntry(RequestDetails theRequestDetails, BundleEntryComponent entry) {
        List<IBaseResource> preExistResources = resourcePreExist(theRequestDetails, entry.getResource());
        if (entry.getRequest().getMethod().equals(HTTPVerb.PUT) && preExistResources != null) {
            IBaseResource oldResource = preExistResources.get(0);
            // Collections.sort(preExistResources, new Comparator<IBaseResource>() {
            // @Override
            // public int compare(IBaseResource resA, IBaseResource resB) {
            // // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
            // return Integer.valueOf(resA.getMeta().getVersionId()) >
            // Integer.valueOf(resB.getMeta().getVersionId()) ? -1 :
            // (Integer.valueOf(resA.getMeta().getVersionId()) <
            // Integer.valueOf(resB.getMeta().getVersionId())) ? 1 : 0;
            // }
            // });
            IBaseResource newResource = entry.getResource();
            if (contentChanged(oldResource, newResource))
                new AuditTrailInterceptor().resourcePreUpdate(theRequestDetails, preExistResources.get(0),
                        entry.getResource());
            return;
        }

        if (entry.getRequest().getMethod().equals(HTTPVerb.POST) || entry.getRequest().getMethod().equals(HTTPVerb.PUT))
            new AuditTrailInterceptor().resourcePreCreate(theRequestDetails, entry.getResource());
    }

    private static <T extends IBaseResource> List<IBaseResource> resourcePreExist(RequestDetails theRequestDetails,
            Resource resource) {
        try {
            PlainSystemProviderR4 plainSystemProvider = new PlainSystemProviderR4();
            IFhirResourceDao<T> resourceDAO = plainSystemProvider
                    .getDao(new StringType(resource.getResourceType().name()));
            IdType resourceId = new IdType(resource.getIdElement().getIdPart());
            IBundleProvider preExist = plainSystemProvider.instanceHistory(theRequestDetails, resourceDAO, resourceId);
            if (preExist.size() == null || preExist.size() < 1) {
                return null;
            }
            return preExist.getResources(0, preExist.size());
        } catch (ResourceNotFoundException ex) {
            return null;
        }
    }

    // TODO: compare without meta, id, version etc.
    private static <T extends IBaseResource> boolean contentChanged(IBaseResource oldResource,
            IBaseResource newResource) {
        return oldResource.equals(newResource);
    }
}
