package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.starter.dotBase.PlainSystemProviderR4;
import ca.uhn.fhir.jpa.starter.dotBase.interceptors.AuditTrailInterceptor;
import ca.uhn.fhir.jpa.starter.dotBase.utils.DaoUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.DateUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.ExtensionUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.MetaUtils;
import ca.uhn.fhir.jpa.starter.dotBase.utils.ResourceComparator;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditTrail {
  private static final Logger ourLog = LoggerFactory.getLogger(AuditTrail.class);

  public static void setCreationDateTime(RequestDetails theRequest, IBaseResource theResource) {
    DateTimeType now = new DateTimeType(DateUtils.getCurrentTimestamp());
    String system = "https://simplifier.net/dot.base/resource-creation-datetime-namingsystem";
    MetaUtils.setTag(theRequest.getFhirContext(), theResource, system, now.getValueAsString());
  }

  public static void setResourceCreator(RequestDetails theRequest, IBaseResource theResource) {
    String username = getUsername(theRequest);
    String system = "https://simplifier.net/dot.base/dotbase-username-namingsystem";
    MetaUtils.setTag(theRequest.getFhirContext(), theResource, system, username);
  }

  public static void setResourceEditor(RequestDetails theRequest, IBaseResource theResource) {
    String username = getUsername(theRequest);
    String system = "https://simplifier.net/dot.base/resource-editor-username";
    ExtensionUtils.addExtension(theResource, system, username);
  }

  public static void handleTransaction(RequestDetails theRequest) {
    if (theRequest.getResource() instanceof Bundle) {
      Bundle theResource = (Bundle) theRequest.getResource();
      List<BundleEntryComponent> entries = theResource.getEntry();
      entries.forEach(entry -> transactionEntry(theRequest, entry));
    }
  }

  private static void transactionEntry(RequestDetails theRequest, BundleEntryComponent entry) {
    boolean isPut = entry.getRequest().getMethod().equals(HTTPVerb.PUT);
    boolean isPost = entry.getRequest().getMethod().equals(HTTPVerb.POST);
    if (isPut || isPost) {
      setAuditTrail(theRequest, entry);
    }
  }

  private static void setAuditTrail(RequestDetails theRequest, BundleEntryComponent entry) {
    IBaseResource oldResource = resourcePreVersion(theRequest, entry.getResource());
    IBaseResource newResource = entry.getResource();
    boolean resourceDiff = ResourceComparator.hasDiff(theRequest.getFhirContext(), newResource, oldResource);

    if (resourceDiff)
      new AuditTrailInterceptor().resourcePreUpdate(theRequest, oldResource, newResource);
    if (oldResource == null)
      new AuditTrailInterceptor().resourcePreCreate(theRequest, newResource);
  }

  private static <T extends IBaseResource> IBaseResource resourcePreVersion(RequestDetails theRequest,
      Resource resource) {
    try {
      IFhirResourceDao<T> resourceDAO = DaoUtils.getDao(new StringType(resource.getResourceType().name()));
      IdType resourceId = new IdType(resource.getIdElement().getIdPart());
      IBundleProvider preExist = new PlainSystemProviderR4().instanceHistory(theRequest, resourceDAO, resourceId);

      if (preExist.size() != null || preExist.size() > 0) {
        return getLatestVersion(preExist.getResources(0, preExist.size()));
      }
      return null;
    } catch (ResourceNotFoundException ex) {
      return null;
    }
  }

  private static IBaseResource getLatestVersion(List<IBaseResource> preExistResources) {
    Collections.sort(preExistResources, new Comparator<IBaseResource>() {

      public int compare(IBaseResource resA, IBaseResource resB) {
        return resB.getMeta().getVersionId().compareTo(resA.getMeta().getVersionId());
      }
    });
    return preExistResources.get(preExistResources.size() - 1);
  }

  private static String getUsername(RequestDetails theRequest) {
    if (theRequest.getAttribute("_username") == null) {
      return "unknown";
    }
    return theRequest.getAttribute("_username").toString();
  }
}
