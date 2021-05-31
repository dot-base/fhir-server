package ca.uhn.fhir.jpa.starter.dotBase.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.patch.FhirPatch;
import javax.annotation.Nonnull;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.BooleanType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceComparator extends FhirPatch {
  private static final Logger ourLog = LoggerFactory.getLogger(ResourceComparator.class);

  public ResourceComparator(FhirContext theContext) {
    super(theContext);
  }

  public static boolean hasDiff(
    FhirContext fhirContext,
    IBaseResource newResource,
    IBaseResource oldResource
  ) {
    if (oldResource != null) {
      IBaseParameters resourceDiff = ResourceComparator.resourceDiff(
        new BooleanType(false),
        fhirContext,
        newResource,
        oldResource
      );
      return !resourceDiff.isEmpty();
    }
    return false;
  }

  private static IBaseParameters resourceDiff(
    IPrimitiveType<Boolean> theIncludeMeta,
    FhirContext myContext,
    IBaseResource sourceResource,
    IBaseResource targetResource
  ) {
    ResourceComparator comparator = setResourceComparator(theIncludeMeta, myContext);
    return comparator.diff(sourceResource, targetResource);
  }

  @Nonnull
  private static ResourceComparator setResourceComparator(
    IPrimitiveType<Boolean> theIncludeMeta,
    FhirContext myContext
  ) {
    ResourceComparator fhirPatch = new ResourceComparator(myContext);
    fhirPatch.setIncludePreviousValueInDiff(true);
    fhirPatch.addIgnorePath("*.text");
    if (theIncludeMeta != null && theIncludeMeta.getValue()) {
      ourLog.trace("Including resource metadata in patch");
    } else {
      fhirPatch.addIgnorePath("*.meta");
    }

    return fhirPatch;
  }
}
