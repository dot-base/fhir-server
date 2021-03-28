package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.dotBase.api.FhirServer;
import ca.uhn.fhir.jpa.starter.dotBase.services.ExternalReferences;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntrySearchComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Resource;

public class ExternalReferences {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    ExternalReferences.class
  );

  private static final IParser r4Parser = FhirContext.forR4().newJsonParser();

  public static Bundle resolve(Bundle responseBundle, RequestDetails theRequestDetails) {
    HashSet<String> externalReferences = getExternalReferences(theRequestDetails);
    List<BundleEntryComponent> includeEntries = includeEntries(
      externalReferences,
      theRequestDetails
    );
    includeEntries.forEach(entry -> responseBundle.addEntry(entry));
    return responseBundle;
  }

  @SuppressWarnings("unchecked")
  private static HashSet<String> getExternalReferences(RequestDetails theRequestDetails) {
    return (HashSet<String>) theRequestDetails.getAttribute(
      "_includeIsExternalReference"
    );
  }

  private static List<BundleEntryComponent> includeEntries(
    HashSet<String> externalReferences,
    RequestDetails theRequestDetails
  ) {
    List<BundleEntryComponent> entries = new LinkedList<BundleEntryComponent>();
    for (String externalReference : externalReferences) {
      BundleEntryComponent entry = includeEntry(externalReference, theRequestDetails);
      if (entry != null) entries.add(entry);
    }
    return entries;
  }

  private static BundleEntryComponent includeEntry(
    String externalReference,
    RequestDetails theRequestDetails
  ) {
    BundleEntryComponent entry = new BundleEntryComponent();
    Resource resource = getExternalResource(externalReference, theRequestDetails);
    if (resource != null) {
      return entry
        .setResource(resource)
        .setFullUrl(externalReference)
        .setSearch(new BundleEntrySearchComponent().setMode(SearchEntryMode.INCLUDE));
    }
    return null;
  }

  private static Resource getExternalResource(
    String url,
    RequestDetails theRequestDetails
  ) {
    String resource = FhirServer.getExternalResource(url, theRequestDetails);
    if (resource != null) {
      return parseToR4Resource(resource);
    }
    return null;
  }

  private static Resource parseToR4Resource(String resource) {
    try {
      return (Resource) r4Parser.parseResource(resource);
    } catch (DataFormatException e) {
      ourLog.info(e.getMessage());
      return null;
    }
  }
}
