package ca.uhn.fhir.jpa.starter.moveBase;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.searchparam.SearchParameterMap;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.springframework.web.context.ContextLoaderListener;

public class PlainSystemProviderR4 extends JpaSystemProviderR4 {
  private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(
    PlainSystemProviderR4.class
  );

  @SuppressWarnings("unchecked")
  @Operation(name = "$all-versions", idempotent = true)
  public <T extends IBaseResource> Parameters getAllVersions(
    HttpServletRequest theRequest,
    RequestDetails requestDetails,
    @OperationParam(name = "_id") IdType resourceId,
    @OperationParam(name = "type") IdType resourceType
  )
    throws Exception {
    //ADD Method getDaoByType containing switch for gobal types defined in hapi.properties
    IFhirResourceDao<T> resourceDAO = ContextLoaderListener
      .getCurrentWebApplicationContext()
      .getBean("my" + resourceType + "DaoR4", IFhirResourceDao.class);

    SearchParameterMap paramMap = new SearchParameterMap()
    .add("_id", new ReferenceParam(resourceId));
    IBundleProvider searchRes = resourceDAO.search(paramMap, requestDetails);
    List<IBaseResource> resources = searchRes.getResources(0, 1);
    LOGGER.info("resultUUID: " + searchRes.getUuid());
    LOGGER.info("resources no: " + resources.size());

    Parameters retVal = new Parameters();
    resources
      .stream()
      .forEach(
        r ->
          retVal
            .addParameter()
            .setName("updatedConsent")
            .setName(r.getIdElement().getIdPart())
      );
    return retVal;
  }
}
