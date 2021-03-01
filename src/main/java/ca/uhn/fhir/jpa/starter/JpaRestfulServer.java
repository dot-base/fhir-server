package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.dotBase.PlainSystemProviderR4;
import ca.uhn.fhir.jpa.starter.dotBase.ResponseInterceptorExternalReference;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import io.sentry.Sentry;
import javax.servlet.ServletException;

public class JpaRestfulServer extends BaseJpaRestfulServer {
  private static final long serialVersionUID = 1L;

  @Override
  protected void initialize() throws ServletException {
    super.initialize();
    // Add your own customization here

    registerProvider(new PlainSystemProviderR4());
    registerInterceptor(new ResponseInterceptorExternalReference());

    Sentry.init(
      options -> {
        options.setDsn(System.getenv("SENTRY_DSN"));
        options.setEnvironment(System.getenv("SENTRY_ENVIRONMENT"));
        options.setServerName(HapiProperties.getServerName());
        options.setTracesSampleRate(1.0);
        options.setConnectionTimeoutMillis(10000);
        options.setReadTimeoutMillis(10000);
      }
    );

    FhirContext ctx = getFhirContext();
    ctx.getParserOptions().setStripVersionsFromReferences(false);
  }
}
