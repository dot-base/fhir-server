package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.dotBase.PlainSystemProviderR4;
import ca.uhn.fhir.jpa.starter.dotBase.api.IdentityProvider;
import ca.uhn.fhir.jpa.starter.dotBase.interceptors.AuthenticationInterceptor;
import ca.uhn.fhir.jpa.starter.dotBase.interceptors.ResponseInterceptorExternalReference;
import io.sentry.Sentry;
import io.sentry.SentryOptions.Proxy;
import javax.servlet.ServletException;

public class JpaRestfulServer extends BaseJpaRestfulServer {
  private static final long serialVersionUID = 1L;
  private static final String SENTRY_DSN = System.getenv("SENTRY_DSN") == null
    ? ""
    : System.getenv("SENTRY_DSN");
  private static final String SENTRY_ENV = System.getenv("SENTRY_ENVIRONMENT");

  @Override
  protected void initialize() throws ServletException {
    super.initialize();
    // Add your own customization here

    registerProvider(new PlainSystemProviderR4());
    registerInterceptor(new ResponseInterceptorExternalReference());
    if (HapiProperties.isAuthenticationInterceptorEnabled()) {
      setRealmPublicKey();
      registerInterceptor(new AuthenticationInterceptor());
    }

    Sentry.init(
      options -> {
        options.setDsn(SENTRY_DSN);
        options.setEnvironment(SENTRY_ENV);
        options.setProxy(new Proxy("http://proxy.charite.de", "8080"));
        options.setServerName(HapiProperties.getServerName());
        options.setTracesSampleRate(1.0);
        options.setConnectionTimeoutMillis(10000);
        options.setReadTimeoutMillis(10000);
      }
    );

    FhirContext ctx = getFhirContext();
    ctx.getParserOptions().setStripVersionsFromReferences(false);
  }

  public static void setRealmPublicKey() {
    String realm = HapiProperties.getIdentityProviderRealm();
    HapiProperties.setProperty(
      "REALM_PUBLIC_KEY",
      IdentityProvider.getRealmPublicKey(realm)
    );
  }
}
