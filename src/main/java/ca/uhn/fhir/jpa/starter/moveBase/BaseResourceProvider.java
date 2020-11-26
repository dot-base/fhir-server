package ca.uhn.fhir.jpa.starter.moveBase;

//import ca.uhn.fhir.jpa.provider.r4.JpaResourceProviderR4;
import ca.uhn.fhir.jpa.rp.r4.PatientResourceProvider;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;

//extends JpaResourceProviderR4<IAnyResource>
public class BaseResourceProvider extends PatientResourceProvider {
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(
    BaseResourceProvider.class
  );

  @Operation(name = "$all-instance-versions", idempotent = true)
  public <T extends IAnyResource> Parameters getAllVersions(
    HttpServletRequest theRequest,
    RequestDetails requestDetails,
    @OperationParam(name = "_id") IdType resourceId,
    @OperationParam(name = "since") String since, //Refactor using DateType
    @OperationParam(name = "until") String until ////Refactor using DateType
  )
    throws Exception {
    LOGGER.info("baseRP called: ");

    SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
    String sinceString = "7-Jun-2000";
    String untilString = "30-Nov-2020";
    Date dateSince = formatter.parse(sinceString);
    Date dateUntil = formatter.parse(untilString);
    DateRangeParam at = new DateRangeParam(dateSince, dateUntil);
    IIdType id = new IdType("/Patient/703");

    IBundleProvider resourceHistory =
      this.getHistoryForResourceInstance(theRequest, id, dateSince, at, requestDetails);
    List<IBaseResource> resources = resourceHistory.getResources(0, 1);
    LOGGER.info("resultUUID: " + resourceHistory.getUuid());
    LOGGER.info("resources no: " + resources.size());

    Parameters retVal = new Parameters();
    resources
      .stream()
      .forEach(
        r ->
          retVal
            .addParameter()
            .setName(r.getIdElement().getValueAsString())
            .setName(r.getIdElement().getIdPart())
      );
    return retVal;
  }
}
