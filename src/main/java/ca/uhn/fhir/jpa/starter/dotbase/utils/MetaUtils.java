package ca.uhn.fhir.jpa.starter.dotbase.utils;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.Random;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class MetaUtils {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(MetaUtils.class);

  public static void setTag(FhirContext theContext, IBaseResource theResource, String theSystem, String theValue) {

    IBaseMetaType theMeta = theResource.getMeta();
    BaseRuntimeElementCompositeDefinition<?> elementDef = (BaseRuntimeElementCompositeDefinition<?>) theContext
        .getElementDefinition(theMeta.getClass());
    BaseRuntimeChildDefinition sourceChild = elementDef.getChildByName("tag");
    List<IBase> tagValues = sourceChild.getAccessor().getValues(theMeta);
    IBaseCoding tagElement = (IBaseCoding) theContext.getElementDefinition("Coding").newInstance();

    if (tagValues.size() > 0) {
      tagValues.add(tagElement);
    } else {
      sourceChild.getMutator().setValue(theMeta, tagElement);
    }

    String theCode = getTagElementCode(theValue);
    tagElement.setSystem(theSystem);
    tagElement.setDisplay(theValue);
    tagElement.setCode(theCode);
  }

  /**
   * --- Generates a unique value for meta.tag.code --- Otherwise duplicate values
   * in table hfj_tag_def occure, which leads to HTTP 500 being returned with
   * error message "ConstraintViolationException: could not execute batch"
   */
  private static String getTagElementCode(String theValue) {
    theValue += "-" + DateUtils.getCurrentTimestamp() + "-" + enforceUniqueCode();
    return theValue;
  }

  private static Long enforceUniqueCode() {
    return new Random().nextLong();
  }

}