package ca.uhn.fhir.jpa.starter.dotBase.utils;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class MetaUtils {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(MetaUtils.class);

  public static void setTag(
    FhirContext theContext,
    IBaseResource theResource,
    String theSystem,
    String theValue
  ) {
    IBaseMetaType theMeta = theResource.getMeta();
    BaseRuntimeElementCompositeDefinition<?> elementDef = (BaseRuntimeElementCompositeDefinition<?>) theContext.getElementDefinition(
      theMeta.getClass()
    );
    BaseRuntimeChildDefinition sourceChild = elementDef.getChildByName("tag");
    List<IBase> tagValues = sourceChild.getAccessor().getValues(theMeta);
    IBaseCoding tagElement = (IBaseCoding) theContext.getElementDefinition("Coding").newInstance();
    if (tagValues.size() > 0) {
      tagValues.add(tagElement);
    } else {
      sourceChild.getMutator().setValue(theMeta, tagElement);
    }
    tagElement.setSystem(theSystem);
    tagElement.setCode(theValue);
  }
}
