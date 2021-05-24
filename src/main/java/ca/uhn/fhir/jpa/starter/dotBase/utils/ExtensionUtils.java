package ca.uhn.fhir.jpa.starter.dotBase.utils;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.hl7.fhir.r4.model.StringType;

public class ExtensionUtils {

    public static void addExtension(RequestDetails theRequestDetails, String theUrl, String theValue) {
        IBase resource = (IBase) theRequestDetails.getResource();
        if (resource instanceof IBaseHasExtensions) {
            IBaseHasExtensions baseHasExtensions = (IBaseHasExtensions) resource;
            IBaseExtension<?, ?> extension = baseHasExtensions.addExtension();
            extension.setUrl(theUrl);
            extension.setValue(new StringType(theValue));
        }
    }
}
