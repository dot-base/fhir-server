package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Procedure.ProcedureStatus;
import org.hl7.fhir.r4.model.ResourceType;

public class Authorization implements IConsentService {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(Authorization.class);

  private static final Set<String> DRAFT_RESOURCE_TYPES;

  //TODO: delimit resourceTypes or include all types? If so missing draft resourceTypes?
  static {
    DRAFT_RESOURCE_TYPES = new HashSet<String>();
    DRAFT_RESOURCE_TYPES.add(ResourceType.Procedure.toString());
    DRAFT_RESOURCE_TYPES.add(ResourceType.QuestionnaireResponse.toString());
    DRAFT_RESOURCE_TYPES.add(ResourceType.Condition.toString());
  }

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

  private boolean isDraftResource(IBaseResource theResource) {
    if (DRAFT_RESOURCE_TYPES.contains(theResource.fhirType())) {
      return isDraft(theResource);
    }
    return false;
  }

  private static boolean isDraft(IBaseResource theResource) {
    if (theResource instanceof Procedure) {
      Procedure procedure = (Procedure) theResource;
      return procedure.getStatus() == ProcedureStatus.INPROGRESS;
    }
    return ((Element) theResource).getExtensionByUrl("https://simplifier.net/dot.base/draft-action")!=null;
  }

  private boolean isAuthorizedRequester(RequestDetails theRequestDetails, DomainResource theResource) {
    String requestingUser = (String) theRequestDetails.getAttribute("_username");
    return requesterOwnsDraftResource(theResource, requestingUser);
  }

  private static boolean requesterOwnsDraftResource(DomainResource theResource, String requestingUser) {
    if (hasMatchingUsernameTag(theResource, requestingUser) != null) {
      return true;
    }
    return false;
  }

  private static Coding hasMatchingUsernameTag(DomainResource theResource, String requestingUser) {
    return theResource
      .getMeta()
      .getTag("https://simplifier.net/dot.base/requesting-username-namingsystem",requestingUser);
  }
}
