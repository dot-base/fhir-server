package ca.uhn.fhir.jpa.starter.dotBase;

import ca.uhn.fhir.context.FhirContext;
/*-
 * #%L
 * HAPI FHIR - Server Framework
 * %%
 * Copyright (C) 2014 - 2020 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.starter.dotBase.api.FhirServer;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntrySearchComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Resource;

/**
 * This interceptor handles Search request with * <code>_include</code> that
 * result in an external reference (e.g. Patients) to be included
 *
 * External resources are fetched here and added to the Outgoing Response
 *
 * @see <a href=
 *      "https://hapifhir.io/hapi-fhir/docs/interceptors/built_in_server_interceptors.html#response-customizing-evaluate-fhirpath">Interceptors
 *      - Response Customization: Evaluate FHIRPath</a>
 * @since 5.0.0
 */
public class ResponseInterceptorExternalReference {

  private static final IParser r4Parser = FhirContext.forR4().newJsonParser();
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory
      .getLogger(ResponseInterceptorExternalReference.class);

  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public void preProcessOutgoingResponse(RequestDetails theRequestDetails, ResponseDetails theResponseDetails) {
    IBaseResource responseResource = theResponseDetails.getResponseResource();
    if (responseResource != null && this.hasExternalReference(theRequestDetails)) {
      Bundle responseBundle = this.getModifiedResponse((Bundle) responseResource, theRequestDetails);
      theResponseDetails.setResponseResource(responseBundle);
    }
  }

  private boolean hasExternalReference(RequestDetails theRequestDetails) {
    if (theRequestDetails.getAttribute("_includeIsExternalReference") != null) {
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private HashSet<String> getExternalReferences(RequestDetails theRequestDetails) {
    return (HashSet<String>) theRequestDetails.getAttribute("_includeIsExternalReference");
  }

  private Bundle getModifiedResponse(Bundle responseBundle, RequestDetails theRequestDetails) {
    HashSet<String> externalReferences = this.getExternalReferences(theRequestDetails);
    List<BundleEntryComponent> includeEntries = this.includeEntries(externalReferences, theRequestDetails);
    includeEntries.forEach(entry -> responseBundle.addEntry(entry));
    return responseBundle;
  }

  private List<BundleEntryComponent> includeEntries(HashSet<String> externalReferences,
      RequestDetails theRequestDetails) {
    List<BundleEntryComponent> entries = new LinkedList<BundleEntryComponent>();
    for (String externalReference : externalReferences) {
      BundleEntryComponent entry = this.includeEntry(externalReference, theRequestDetails);
      if (entry != null)
        entries.add(entry);
    }
    return entries;
  }

  private BundleEntryComponent includeEntry(String externalReference, RequestDetails theRequestDetails) {
    BundleEntryComponent entry = new BundleEntryComponent();
    Resource resource = this.getExternalResource(externalReference, theRequestDetails);
    if (resource != null) {
      return entry
        .setResource(resource)
        .setFullUrl(externalReference)
        .setSearch(new BundleEntrySearchComponent().setMode(SearchEntryMode.INCLUDE));
    }
    return null;
  }

  private Resource getExternalResource(String url, RequestDetails theRequestDetails) {
    String resource = FhirServer.getExternalResource(url, theRequestDetails);
    if (resource != null) {
      return parseToPatient(resource);
    }
    return null;
  }

  private static Patient parseToPatient(String resource) {
    try {
      return r4Parser.parseResource(Patient.class, resource);
    } catch (DataFormatException e) {
      ourLog.info(e.getMessage());
      return null;
    }
  }
}
