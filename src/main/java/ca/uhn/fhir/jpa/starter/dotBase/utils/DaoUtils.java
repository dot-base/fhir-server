package ca.uhn.fhir.jpa.starter.dotBase.utils;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.web.context.ContextLoaderListener;

public class DaoUtils {

  @SuppressWarnings("unchecked")
  public static <T extends IBaseResource> IFhirResourceDao<T> getDao(
    StringType resourceType
  ) {
    return ContextLoaderListener
      .getCurrentWebApplicationContext()
      .getBean("my" + resourceType + "DaoR4", IFhirResourceDao.class);
  }
}
