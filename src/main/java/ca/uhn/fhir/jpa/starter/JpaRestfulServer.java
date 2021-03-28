package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.dotBase.PlainSystemProviderR4;
import ca.uhn.fhir.jpa.starter.dotBase.api.IdentityProvider;
import ca.uhn.fhir.jpa.starter.dotBase.interceptors.AuthenticationInterceptor;
import ca.uhn.fhir.jpa.starter.dotBase.interceptors.ResponseInterceptor;
import ca.uhn.fhir.jpa.starter.dotBase.services.Authorization;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentInterceptor;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import io.sentry.Sentry;
import io.sentry.SentryOptions.Proxy;
import javax.servlet.ServletException;

public class JpaRestfulServer extends BaseJpaRestfulServer {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    JpaRestfulServer.class
  );

  private static final long serialVersionUID = 1L;
  private static final String SENTRY_DSN = System.getenv("SENTRY_DSN") == null
    ? ""
    : System.getenv("SENTRY_DSN");
  private static final String SENTRY_ENV = System.getenv("SENTRY_ENVIRONMENT");
  private static final String PROXY_ADDRESS = System.getenv("PROXY_ADDRESS");
  private static final String PROXY_PORT = System.getenv("PROXY_PORT");

  @Override
  protected void initialize() throws ServletException {
    super.initialize();
    // Add your own customization here

    registerProvider(new PlainSystemProviderR4());

    registerInterceptor(new ResponseInterceptor());

    IConsentService authorizationService = new Authorization();
    ConsentInterceptor consentInterceptor = new ConsentInterceptor();
    consentInterceptor.setConsentService(authorizationService);
    registerInterceptor(consentInterceptor);

    if (HapiProperties.isAuthenticationInterceptorEnabled()) {
      setRealmPublicKey();
      registerInterceptor(new AuthenticationInterceptor());
    }

    if (HapiProperties.isErrorMonitorinEnabled()) {
      Sentry.init(
        options -> {
          options.setDsn(SENTRY_DSN);
          options.setEnvironment(SENTRY_ENV);
          options.setProxy(new Proxy(PROXY_ADDRESS, PROXY_PORT));
          options.setServerName(HapiProperties.getServerName());
          options.setTracesSampleRate(1.0);
          options.setConnectionTimeoutMillis(10000);
          options.setReadTimeoutMillis(10000);
        }
      );
    }

    FhirContext ctx = getFhirContext();
    ctx.getParserOptions().setStripVersionsFromReferences(false);
  }

  private static void setRealmPublicKey() {
    String realm = HapiProperties.getIdentityProviderRealm();
    HapiProperties.setProperty(
      "sso_realm.publicKey",
      IdentityProvider.getRealmPublicKey(realm)
    );
  }
}
