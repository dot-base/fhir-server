package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.jpa.api.dao.IDao;
import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.dao.SearchBuilder;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.starter.dotBase.searchBuilder.DotBaseSearchBuilder;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

@Configuration
public class FhirServerConfigR4 extends BaseJavaConfigR4 {
  @Autowired
  private DataSource myDataSource;

  /**
   * We override the paging provider definition so that we can customize
   * the default/max page sizes for search results. You can set these however
   * you want, although very large page sizes will require a lot of RAM.
   */
  @Override
  public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
    DatabaseBackedPagingProvider pagingProvider = super.databaseBackedPagingProvider();
    pagingProvider.setDefaultPageSize(HapiProperties.getDefaultPageSize());
    pagingProvider.setMaximumPageSize(HapiProperties.getMaximumPageSize());
    return pagingProvider;
  }

  @Bean(name = SEARCH_BUILDER)
  @Scope("prototype")
  public SearchBuilder persistedJpaSearchFirstPageBundleProvider(
    IDao theDao,
    String theResourceName,
    Class<? extends IBaseResource> theResourceType
  ) {
    return new DotBaseSearchBuilder(theDao, theResourceName, theResourceType);
  }

  @Override
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean retVal = super.entityManagerFactory();
    retVal.setPersistenceUnitName("HAPI_PU");

    try {
      retVal.setDataSource(myDataSource);
    } catch (Exception e) {
      throw new ConfigurationException(
        "Could not set the data source due to a configuration issue",
        e
      );
    }

    retVal.setJpaProperties(HapiProperties.getJpaProperties());
    return retVal;
  }

  @Bean
  @Primary
  public JpaTransactionManager hapiTransactionManager(
    EntityManagerFactory entityManagerFactory
  ) {
    JpaTransactionManager retVal = new JpaTransactionManager();
    retVal.setEntityManagerFactory(entityManagerFactory);
    return retVal;
  }
}
