package ca.uhn.fhir.jpa.starter.dotbase.services;

import ca.uhn.fhir.jpa.starter.dotbase.DotbaseProperties.ResourceUrls;
import ca.uhn.fhir.jpa.starter.dotbase.utils.ExtensionUtils;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
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
      IConsentContextServices theContextServices) {
    if (theRequestDetails.getRequestType() == RequestTypeEnum.GET && isDraftResource(theResource)) {
      return isAuthorizedRequester(theRequestDetails, theResource) ? ConsentOutcome.AUTHORIZED : ConsentOutcome.REJECT;
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
    boolean isDraft = isDraft(theResource);
    if (!isDraft && theResource instanceof Procedure) {
      Procedure procedure = (Procedure) theResource;
      return procedure.getStatus() == ProcedureStatus.INPROGRESS;
    }
    return isDraft;
  }

  private static boolean isDraft(IBaseResource theResource) {
    String theExtensionUrl = ResourceUrls.structuredefinition_document_draft_action;
    return ExtensionUtils.hasExtension(theResource, theExtensionUrl);
  }

  private boolean isAuthorizedRequester(RequestDetails theRequestDetails, IBaseResource theResource) {
    String requestingUser = (String) theRequestDetails.getAttribute("_username");
    return (isResourceCreator(theResource, requestingUser) || isResourceEditor(theResource, requestingUser));
  }

  private static boolean isResourceCreator(IBaseResource theResource, String requestingUser) {
    for (IBaseCoding tag : theResource.getMeta().getTag()) {
      if (tag.getDisplay().equals(requestingUser)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isResourceEditor(IBaseResource theResource, String requestingUser) {
    String theExtensionUrl = ResourceUrls.structuredefinition_resource_editor;
    return ExtensionUtils.hasExtension((IBase) theResource, theExtensionUrl, requestingUser);
  }
}
