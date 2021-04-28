package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;

public class Authorization implements IConsentService {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(Authorization.class);

  /**
   * Invoked once at the start of every request
   */
  @Override
  public ConsentOutcome startOperation(RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    return ConsentOutcome.PROCEED;
  }

  /**
   * Can a given resource be returned to the user?
   */
  @Override
  public ConsentOutcome canSeeResource(RequestDetails theRequestDetails, IBaseResource theResource,
    IConsentContextServices theContextServices
  ) {
    if (theRequestDetails.getRequestType() == RequestTypeEnum.GET && isDraftResource(theResource)) {
      return isAuthorizedRequester(theRequestDetails, (DomainResource) theResource)
        ? ConsentOutcome.AUTHORIZED
        : ConsentOutcome.REJECT;
    }
    return ConsentOutcome.AUTHORIZED;
  }

  /**
   * Modify resources that are being shown to the user
   */
  @Override
  public ConsentOutcome willSeeResource(RequestDetails theRequestDetails, IBaseResource theResource,
      IConsentContextServices theContextServices) {
    return ConsentOutcome.AUTHORIZED;
  }

  private static boolean isDraftResource(IBaseResource theResource) {
    boolean isDraft = ((Element) theResource).getExtensionByUrl("https://simplifier.net/dot.base/draft-action")!=null;
    if (!isDraft && theResource instanceof Procedure) {
      Procedure procedure = (Procedure) theResource;
      return procedure.getStatus() == ProcedureStatus.INPROGRESS;
    }
    return isDraft;
  }

  private boolean isAuthorizedRequester(RequestDetails theRequestDetails, DomainResource theResource) {
    String requestingUser = (String) theRequestDetails.getAttribute("_username");
    return isResourceCreator(theResource, requestingUser) || isResourceEditor(theResource, requestingUser);
  }

  private static boolean isResourceCreator(DomainResource theResource, String requestingUser) {
    return (theResource
      .getMeta()
      .getTag("https://simplifier.net/dot.base/dotbase-username-namingsystem", requestingUser)
      != null
    );
  }

  private static boolean isResourceEditor(DomainResource theResource, String requestingUser) {
    Extension usernameExtension = theResource.getExtensionByUrl("https://simplifier.net/dot.base/resource-editor-username");
    if (usernameExtension != null) {
      return usernameExtension.getValue().toString().equals(requestingUser);
    }
    return false;
  }
}
