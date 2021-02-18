package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.dotBase.PatientInterceptor;
import ca.uhn.fhir.jpa.starter.dotBase.PatientResponseInterceptor;
import ca.uhn.fhir.jpa.starter.dotBase.PlainSystemProviderR4;
import javax.servlet.ServletException;

public class JpaRestfulServer extends BaseJpaRestfulServer {
  private static final long serialVersionUID = 1L;

  @Override
  protected void initialize() throws ServletException {
    super.initialize();
    // Add your own customization here

    registerProvider(new PlainSystemProviderR4());
    registerInterceptor(new PatientInterceptor());
    registerInterceptor(new PatientResponseInterceptor());

    FhirContext ctx = getFhirContext();
    ctx.getParserOptions().setStripVersionsFromReferences(false);
  }
}
