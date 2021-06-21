package ca.uhn.fhir.jpa.starter.dotbase.utils;

import ca.uhn.fhir.jpa.api.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.starter.dotbase.ApplicationContextProvider;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.context.ApplicationContext;

public class DaoUtils {

  private static final ApplicationContext APP_CTX = ApplicationContextProvider.getApplicationContext();
  
  @SuppressWarnings("unchecked")
  public <T extends IBaseResource> IFhirResourceDao<T> getDao(StringType resourceType) {
    return APP_CTX.getBean("my" + resourceType + "DaoR4", IFhirResourceDao.class);
  }

}
