package ca.uhn.fhir.jpa.starter.dotBase.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

public class FhirServer {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    FhirServer.class
  );
  private static final HttpClient HTTP_CLIENT = FhirServer.clientConfig();

  private static final HttpClient clientConfig() {
    return HttpClient
      .newBuilder()
      .version(Version.HTTP_2)
      .connectTimeout(Duration.ofSeconds(30))
      .followRedirects(Redirect.NEVER)
      .build();
  }

  public static String getExternalResource(String uri) {
    HttpResponse<String> response = null;
    HttpRequest request = HttpRequest
      .newBuilder()
      .uri(URI.create(uri))
      .setHeader("Content-Type", "application/fhir+json")
      .setHeader("Accept", "application/fhir+json; fhirVersion=4.0")
      .GET()
      .build();
    try {
      response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      ourLog.info(e.getMessage());
    }
    return (response.statusCode() == 200 ? response.body() : null);
  }
}
