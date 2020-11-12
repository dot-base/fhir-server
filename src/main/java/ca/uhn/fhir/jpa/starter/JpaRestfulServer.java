package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.context.FhirContext;
import javax.servlet.ServletException;

public class JpaRestfulServer extends BaseJpaRestfulServer {
  private static final long serialVersionUID = 1L;

  @Override
  protected void initialize() throws ServletException {
    super.initialize();
    // Add your own customization here

    FhirContext ctx = getFhirContext();
    ctx.getParserOptions().setStripVersionsFromReferences(false);

  }
}
