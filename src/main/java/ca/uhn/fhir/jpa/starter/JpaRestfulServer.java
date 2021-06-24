package ca.uhn.fhir.jpa.starter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ca.uhn.fhir.jpa.starter.dotbase.DotbaseProperties;
import ca.uhn.fhir.jpa.starter.dotbase.PlainSystemProviderR4;
import ca.uhn.fhir.jpa.starter.dotbase.api.IdentityProvider;
import ca.uhn.fhir.jpa.starter.dotbase.interceptor.AuthenticationInterceptor;
import ca.uhn.fhir.jpa.starter.dotbase.interceptor.AuditTrailInterceptor;
import ca.uhn.fhir.jpa.starter.dotbase.interceptor.ResponseInterceptor;
import ca.uhn.fhir.jpa.starter.dotbase.interceptor.UserRoleInterceptor;
import ca.uhn.fhir.jpa.starter.dotbase.services.Authorization;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentInterceptor;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;

import javax.servlet.ServletException;
import io.sentry.Sentry;
import io.sentry.SentryOptions.Proxy;

@Import({ AppProperties.class, DotbaseProperties.class })
public class JpaRestfulServer extends BaseJpaRestfulServer {

  @Autowired
  AppProperties appProperties;

  @Autowired
  DotbaseProperties dotbaseProperties;

  private static final long serialVersionUID = 1L;
  private static final String SENTRY_DSN = System.getenv("SENTRY_DSN") == null ? "" : System.getenv("SENTRY_DSN");
  private static final String SENTRY_ENV = System.getenv("SENTRY_ENVIRONMENT");
  private static final String PROXY_ADDRESS = System.getenv("PROXY_ADDRESS");
  private static final String PROXY_PORT = System.getenv("PROXY_PORT");

  public JpaRestfulServer() {
    super();
  }

  @Override
  protected void initialize() throws ServletException {
    super.initialize();

    // Add your own customization here

    if (dotbaseProperties.getError_monitoring_enabled()) {
      Sentry.init(options -> {
        options.setDsn(SENTRY_DSN);
        options.setEnvironment(SENTRY_ENV);
        options.setProxy(new Proxy(PROXY_ADDRESS, PROXY_PORT));
        options.setServerName(dotbaseProperties.getServer_name());
        options.setTracesSampleRate(1.0);
        options.setConnectionTimeoutMillis(10000);
        options.setReadTimeoutMillis(10000);
      });
    }

    registerProvider(new PlainSystemProviderR4());
    registerInterceptor(new ResponseInterceptor());
    registerInterceptor(new AuditTrailInterceptor());

    if (dotbaseProperties.getAuthenticationInterceptorEnabled()) {
      String realmPubKey = IdentityProvider.getRealmPublicKey(dotbaseProperties.getIdentityProviderRealm());
      dotbaseProperties.setRealmPublicKey(realmPubKey);

      registerInterceptor(new AuthenticationInterceptor());
      registerInterceptor(new UserRoleInterceptor());

      IConsentService authorizationService = new Authorization();
      ConsentInterceptor consentInterceptor = new ConsentInterceptor();
      consentInterceptor.setConsentService(authorizationService);
      registerInterceptor(consentInterceptor);
    }

  }
}
