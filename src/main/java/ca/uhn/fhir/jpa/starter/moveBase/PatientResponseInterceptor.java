package ca.uhn.fhir.jpa.starter.moveBase;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.ParametersUtil;
import java.util.List;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

/**
 * This interceptor looks for a URL parameter on requests called
 * <code>_include</code> and replaces the resource being returned XXXXX the
 * resource that would otherwise have been returned.
 *
 * @see <a href=
 *      "https://hapifhir.io/hapi-fhir/docs/interceptors/built_in_server_interceptors.html#response-customizing-evaluate-fhirpath">Interceptors
 *      - Response Customization: Evaluate FHIRPath</a>
 * @since 5.0.0
 */

public class PatientResponseInterceptor {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    PatientResponseInterceptor.class
  );

  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public void preProcessOutgoingResponse(
    RequestDetails theRequestDetails,
    ResponseDetails theResponseDetails
  ) {
    IBaseResource responseResource = theResponseDetails.getResponseResource();
    if (responseResource != null) {
      // TODO: ADD Constants.PARAM_INCLUDE_QUALIFIER_RECURSE,
      // PARAM_INCLUDE_QUALIFIER_ITERATE, PARAM_INCLUDE_RECURSE and
      // PARAM_INCLUDE_ITERATE
      String externalReference = theRequestDetails
        .getAttribute("includesExternalReference")
        .toString();
      if (externalReference != null) {
        // List<IBaseResource> bundleList = BundleUtil.toListOfResources(
        // ctx,
        // (IBaseBundle) responseResource
        // );
        FhirContext ctx = theRequestDetails.getFhirContext();
        List<Pair<String, IBaseResource>> bundle = BundleUtil.getBundleEntryUrlsAndResources(
          ctx,
          (IBaseBundle) responseResource
        );
        Patient patient = new Patient();
        patient.addName(new HumanName().setFamily("PatientFamilyName"));
        Pair<String, IBaseResource> includeEntry = new MutablePair<>(
          externalReference,
          patient
        );
        bundle.add(includeEntry);
        Bundle copyBundle = new Bundle();
        bundle.forEach(
          entry -> {
            copyBundle.addEntry(
              new BundleEntryComponent()
                .setFullUrl(entry.getKey())
                .setResource((Resource) entry.getValue())
            );
          }
        );

        theResponseDetails.setResponseResource(copyBundle);
      }
    }
  }
}
