package ca.uhn.fhir.jpa.starter.moveBase;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.IPreResourceAccessDetails;
import ca.uhn.fhir.rest.api.server.IPreResourceShowDetails;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Spliterator;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;

@Interceptor
public class PatientInterceptor {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    PatientInterceptor.class
  );

  @Hook(Pointcut.STORAGE_PRESEARCH_REGISTERED)
  public void patientRefsPreSearch(
    ServletRequestDetails theRequest,
    RequestDetails requestDetails
  ) {
    ourLog.info("STORAGE_PRESEARCH_REGISTERED");
  }

  @Hook(Pointcut.STORAGE_PREACCESS_RESOURCES)
  public void patientRefsPreAccess(
    ServletRequestDetails theRequest,
    RequestDetails requestDetails,
    IPreResourceAccessDetails resourceDetails
  ) {
    // setDontReturnResourceAtIndex for URLs and trigger fetch here
    // resourceDetails.setDontReturnResourceAtIndex(0);
    ourLog.info("STORAGE_PREACCESS_RESOURCES");
  }

  @Hook(Pointcut.STORAGE_PRESHOW_RESOURCES)
  public void patientRefsPreShow(
    HttpServletRequest theRequest,
    RequestDetails requestDetails,
    IPreResourceShowDetails resourceDetails
  ) {
    ourLog.info("STORAGE_PRESHOW_RESOURCES");
  }
}
