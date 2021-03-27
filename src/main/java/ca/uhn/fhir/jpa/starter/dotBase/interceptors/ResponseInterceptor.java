package ca.uhn.fhir.jpa.starter.dotBase.interceptors;

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
import ca.uhn.fhir.jpa.starter.dotBase.services.ExternalReferences;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;

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
public class ResponseInterceptor {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory
      .getLogger(ResponseInterceptor.class);

  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public void preProcessOutgoingResponse(RequestDetails theRequestDetails, ResponseDetails theResponseDetails) {
    IBaseResource responseResource = theResponseDetails.getResponseResource();
    
    if (responseResource != null && this.hasExternalReference(theRequestDetails)) {
      Bundle responseBundle = ExternalReferences.resolve((Bundle) responseResource, theRequestDetails);
      theResponseDetails.setResponseResource(responseBundle);
    }
  }

  private boolean hasExternalReference(RequestDetails theRequestDetails) {
    if (theRequestDetails.getAttribute("_includeIsExternalReference") != null) {
      return true;
    }
    return false;
  }

}
