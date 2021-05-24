package ca.uhn.fhir.jpa.starter.dotBase.utils;

import ca.uhn.fhir.context.BaseRuntimeChildDefinition;
import ca.uhn.fhir.context.BaseRuntimeElementCompositeDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseMetaType;

public class MetaUtils {
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
        MetaUtils.class
      );

    public static void setTag(
        RequestDetails theRequestDetails,
        String theSystem,
        String theValue
      ) {
        FhirContext theContext = theRequestDetails.getFhirContext();
        IBaseMetaType theMeta = theRequestDetails.getResource().getMeta();
        BaseRuntimeElementCompositeDefinition<?> elementDef = (BaseRuntimeElementCompositeDefinition<?>) theContext.getElementDefinition(
          theMeta.getClass()
        );
        BaseRuntimeChildDefinition sourceChild = elementDef.getChildByName("tag");
        List<IBase> tagValues = sourceChild.getAccessor().getValues(theMeta);
        IBaseCoding tagElement;
        if (tagValues.size() > 0) {
          tagElement = (IBaseCoding) tagValues.get(0);
        } else {
          tagElement = (IBaseCoding) theContext.getElementDefinition("Coding").newInstance();
          sourceChild.getMutator().setValue(theMeta, tagElement);
        }
        tagElement.setSystem(theSystem);
        tagElement.setCode(theValue);
      }
}
