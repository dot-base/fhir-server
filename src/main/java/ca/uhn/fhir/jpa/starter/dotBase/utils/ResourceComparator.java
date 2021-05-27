package ca.uhn.fhir.jpa.starter.dotBase.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.patch.FhirPatch;
import io.micrometer.core.lang.Nullable;
import javax.annotation.Nonnull;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceComparator extends FhirPatch {
  private static final Logger ourLog = LoggerFactory.getLogger(ResourceComparator.class);

  public ResourceComparator(FhirContext theContext) {
    super(theContext);
  }

  //TODO: exclude extension resource-editor from comparison
  public IBaseParameters callDiff(
    IPrimitiveType<Boolean> theIncludeMeta,
    FhirContext myContext,
    IBaseResource sourceResource,
    IBaseResource targetResource
  ) {
    ResourceComparator fhirPatch = newPatch(theIncludeMeta, myContext);
    IBaseParameters diff = fhirPatch.diff(sourceResource, targetResource);
    return diff;
  }

  @Nonnull
  public ResourceComparator newPatch(
    IPrimitiveType<Boolean> theIncludeMeta,
    FhirContext myContext
  ) {
    ResourceComparator fhirPatch = new ResourceComparator(myContext);
    fhirPatch.setIncludePreviousValueInDiff(true);

    if (theIncludeMeta != null && theIncludeMeta.getValue()) {
      ourLog.trace("Including resource metadata in patch");
    } else {
      fhirPatch.addIgnorePath("*.meta");
    }

    return fhirPatch;
  }
}
