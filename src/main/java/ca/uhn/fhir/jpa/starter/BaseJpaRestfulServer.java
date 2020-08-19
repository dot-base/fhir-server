package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.IInterceptorBroadcaster;
import ca.uhn.fhir.interceptor.api.IInterceptorService;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.binstore.BinaryStorageInterceptor;
import ca.uhn.fhir.jpa.bulk.provider.BulkDataExportProvider;
import ca.uhn.fhir.jpa.interceptor.CascadingDeleteInterceptor;
import ca.uhn.fhir.jpa.partition.PartitionManagementProvider;
import ca.uhn.fhir.jpa.provider.GraphQLProvider;
import ca.uhn.fhir.jpa.provider.SubscriptionTriggeringProvider;
import ca.uhn.fhir.jpa.provider.TerminologyUploaderProvider;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.jpa.subscription.util.SubscriptionDebugLogInterceptor;
import ca.uhn.fhir.narrative.DefaultThymeleafNarrativeGenerator;
import ca.uhn.fhir.rest.server.HardcodedServerAddressStrategy;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.FhirPathFilterInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.RequestValidatingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseValidatingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.partition.RequestTenantPartitionInterceptor;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.tenant.UrlBaseTenantIdentificationStrategy;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletException;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;

public class BaseJpaRestfulServer extends RestfulServer {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("unchecked")
  @Override
  protected void initialize() throws ServletException {
    super.initialize();

    /*
     * Create a FhirContext object that uses the version of FHIR
     * specified in the properties file.
     */
    ApplicationContext appCtx = (ApplicationContext) getServletContext()
      .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");
    // Customize supported resource types
    Set<String> supportedResourceTypes = HapiProperties.getSupportedResourceTypes();

    if (
      !supportedResourceTypes.isEmpty() &&
      !supportedResourceTypes.contains("SearchParameter")
    ) {
      supportedResourceTypes.add("SearchParameter");
    }

    if (!supportedResourceTypes.isEmpty()) {
      DaoRegistry daoRegistry = appCtx.getBean(DaoRegistry.class);
      daoRegistry.setSupportedResourceTypes(supportedResourceTypes);
    }

    /*
     * ResourceProviders are fetched from the Spring context
     */
    ResourceProviderFactory resourceProviders = appCtx.getBean(
      "myResourceProvidersR4",
      ResourceProviderFactory.class
    );
    Object systemProvider = appCtx.getBean(
      "mySystemProviderR4",
      JpaSystemProviderR4.class
    );

    setFhirContext(appCtx.getBean(FhirContext.class));

    registerProviders(resourceProviders.createProviders());
    registerProvider(systemProvider);

    /*
     * The conformance provider exports the supported resources, search parameters, etc for
     * this server. The JPA version adds resourceProviders counts to the exported statement, so it
     * is a nice addition.
     *
     * You can also create your own subclass of the conformance provider if you need to
     * provide further customization of your server's CapabilityStatement
     */
    DaoConfig daoConfig = appCtx.getBean(DaoConfig.class);
    ISearchParamRegistry searchParamRegistry = appCtx.getBean(ISearchParamRegistry.class);

    IFhirSystemDao<org.hl7.fhir.r4.model.Bundle, org.hl7.fhir.r4.model.Meta> systemDao = appCtx.getBean(
      "mySystemDaoR4",
      IFhirSystemDao.class
    );
    JpaConformanceProviderR4 confProvider = new JpaConformanceProviderR4(
      this,
      systemDao,
      daoConfig,
      searchParamRegistry
    );
    confProvider.setImplementationDescription("HAPI FHIR R4 Server");
    setServerConformanceProvider(confProvider);

    /*
     * ETag Support
     */
    setETagSupport(HapiProperties.getEtagSupport());

    /*
     * This server tries to dynamically generate narratives
     */
    FhirContext ctx = getFhirContext();
    ctx.setNarrativeGenerator(new DefaultThymeleafNarrativeGenerator());

    /*
     * Default to JSON and pretty printing
     */
    setDefaultPrettyPrint(HapiProperties.getDefaultPrettyPrint());

    /*
     * Default encoding
     */
    setDefaultResponseEncoding(HapiProperties.getDefaultEncoding());

    /*
     * This configures the server to page search results to and from
     * the database, instead of only paging them to memory. This may mean
     * a performance hit when performing searches that return lots of results,
     * but makes the server much more scalable.
     */
    setPagingProvider(appCtx.getBean(DatabaseBackedPagingProvider.class));

    /*
     * This interceptor formats the output using nice colourful
     * HTML output when the request is detected to come from a
     * browser.
     */
    ResponseHighlighterInterceptor responseHighlighterInterceptor = new ResponseHighlighterInterceptor();
    this.registerInterceptor(responseHighlighterInterceptor);

    if (HapiProperties.isFhirPathFilterInterceptorEnabled()) {
      registerInterceptor(new FhirPathFilterInterceptor());
    }

    /*
     * Add some logging for each request
     */
    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLoggerName(HapiProperties.getLoggerName());
    loggingInterceptor.setMessageFormat(HapiProperties.getLoggerFormat());
    loggingInterceptor.setErrorMessageFormat(HapiProperties.getLoggerErrorFormat());
    loggingInterceptor.setLogExceptions(HapiProperties.getLoggerLogExceptions());
    this.registerInterceptor(loggingInterceptor);

    /*
     * If you are hosting this server at a specific DNS name, the server will try to
     * figure out the FHIR base URL based on what the web container tells it, but
     * this doesn't always work. If you are setting links in your search bundles that
     * just refer to "localhost", you might want to use a server address strategy:
     */
    String serverAddress = HapiProperties.getServerAddress();
    if (serverAddress != null && serverAddress.length() > 0) {
      setServerAddressStrategy(new HardcodedServerAddressStrategy(serverAddress));
    }

    /*
     * If you are using DSTU3+, you may want to add a terminology uploader, which allows
     * uploading of external terminologies such as Snomed CT. Note that this uploader
     * does not have any security attached (any anonymous user may use it by default)
     * so it is a potential security vulnerability. Consider using an AuthorizationInterceptor
     * with this feature.
     */
    if (false) { // <-- DISABLED RIGHT NOW
      registerProvider(appCtx.getBean(TerminologyUploaderProvider.class));
    }

    // If you want to enable the $trigger-subscription operation to allow
    // manual triggering of a subscription delivery, enable this provider
    if (false) { // <-- DISABLED RIGHT NOW
      SubscriptionTriggeringProvider retriggeringProvider = appCtx.getBean(
        SubscriptionTriggeringProvider.class
      );
      registerProvider(retriggeringProvider);
    }

    // Define your CORS configuration. This is an example
    // showing a typical setup. You should customize this
    // to your specific needs
    if (HapiProperties.getCorsEnabled()) {
      CorsConfiguration config = new CorsConfiguration();
      config.addAllowedHeader(HttpHeaders.ORIGIN);
      config.addAllowedHeader(HttpHeaders.ACCEPT);
      config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
      config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
      config.addAllowedHeader(HttpHeaders.CACHE_CONTROL);
      config.addAllowedHeader("x-fhir-starter");
      config.addAllowedHeader("X-Requested-With");
      config.addAllowedHeader("Prefer");
      String allAllowedCORSOrigins = HapiProperties.getCorsAllowedOrigin();
      Arrays
        .stream(allAllowedCORSOrigins.split(","))
        .forEach(
          o -> {
            config.addAllowedOrigin(o);
          }
        );
      config.addAllowedOrigin(HapiProperties.getCorsAllowedOrigin());

      config.addExposedHeader("Location");
      config.addExposedHeader("Content-Location");
      config.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
      );
      config.setAllowCredentials(HapiProperties.getCorsAllowedCredentials());

      // Create the interceptor and register it
      CorsInterceptor interceptor = new CorsInterceptor(config);
      registerInterceptor(interceptor);
    }

    // If subscriptions are enabled, we want to register the interceptor that
    // will activate them and match results against them
    if (
      HapiProperties.getSubscriptionWebsocketEnabled() ||
      HapiProperties.getSubscriptionEmailEnabled() ||
      HapiProperties.getSubscriptionRestHookEnabled()
    ) {
      // Subscription debug logging
      IInterceptorService interceptorService = appCtx.getBean(IInterceptorService.class);
      interceptorService.registerInterceptor(new SubscriptionDebugLogInterceptor());
    }

    // Cascading deletes
    DaoRegistry daoRegistry = appCtx.getBean(DaoRegistry.class);
    IInterceptorBroadcaster interceptorBroadcaster = appCtx.getBean(
      IInterceptorBroadcaster.class
    );
    if (HapiProperties.getAllowCascadingDeletes()) {
      CascadingDeleteInterceptor cascadingDeleteInterceptor = new CascadingDeleteInterceptor(
        ctx,
        daoRegistry,
        interceptorBroadcaster
      );
      getInterceptorService().registerInterceptor(cascadingDeleteInterceptor);
    }

    // Binary Storage
    if (HapiProperties.isBinaryStorageEnabled()) {
      BinaryStorageInterceptor binaryStorageInterceptor = appCtx.getBean(
        BinaryStorageInterceptor.class
      );
      getInterceptorService().registerInterceptor(binaryStorageInterceptor);
    }

    // Validation
    IValidatorModule validatorModule = appCtx.getBean(IValidatorModule.class);
    if (validatorModule != null) {
      if (HapiProperties.getValidateRequestsEnabled()) {
        RequestValidatingInterceptor interceptor = new RequestValidatingInterceptor();
        interceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
        interceptor.setValidatorModules(Collections.singletonList(validatorModule));
        registerInterceptor(interceptor);
      }
      if (HapiProperties.getValidateResponsesEnabled()) {
        ResponseValidatingInterceptor interceptor = new ResponseValidatingInterceptor();
        interceptor.setFailOnSeverity(ResultSeverityEnum.ERROR);
        interceptor.setValidatorModules(Collections.singletonList(validatorModule));
        registerInterceptor(interceptor);
      }
    }

    // GraphQL
    if (HapiProperties.getGraphqlEnabled()) {
      registerProvider(appCtx.getBean(GraphQLProvider.class));
    }

    if (!HapiProperties.getAllowedBundleTypes().isEmpty()) {
      String allowedBundleTypesString = HapiProperties.getAllowedBundleTypes();
      Set<String> allowedBundleTypes = new HashSet<>();
      Arrays
        .stream(allowedBundleTypesString.split(","))
        .forEach(
          o -> {
            BundleType type = BundleType.valueOf(o);
            allowedBundleTypes.add(type.toCode());
          }
        );
      DaoConfig config = daoConfig;
      config.setBundleTypesAllowedForStorage(
        Collections.unmodifiableSet(new TreeSet<>(allowedBundleTypes))
      );
    }

    // Bulk Export
    if (HapiProperties.getBulkExportEnabled()) {
      registerProvider(appCtx.getBean(BulkDataExportProvider.class));
    }

    // Partitioning
    if (HapiProperties.getPartitioningMultitenancyEnabled()) {
      registerInterceptor(new RequestTenantPartitionInterceptor());
      setTenantIdentificationStrategy(new UrlBaseTenantIdentificationStrategy());
      registerProviders(appCtx.getBean(PartitionManagementProvider.class));
    }

    if (HapiProperties.getClientIdStrategy() == DaoConfig.ClientIdStrategyEnum.ANY) {
      daoConfig.setResourceServerIdStrategy(DaoConfig.IdStrategyEnum.UUID);
      daoConfig.setResourceClientIdStrategy(HapiProperties.getClientIdStrategy());
    }
  }
}
