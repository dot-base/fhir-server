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
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class IdentityProvider {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    IdentityProvider.class
  );
  private static final HttpClient HTTP_CLIENT = IdentityProvider.clientConfig();

  private static final HttpClient clientConfig() {
    return HttpClient
      .newBuilder()
      .version(Version.HTTP_2)
      .connectTimeout(Duration.ofSeconds(30))
      .followRedirects(Redirect.NEVER)
      .build();
  }

  public static String getRealmPublicKey(String realm) {
    try {
      HttpRequest request = HttpRequest
        .newBuilder()
        .uri(URI.create(realm))
        .setHeader("Accept", "application/json")
        .GET()
        .build();
      HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
      return (response.statusCode() == 200 ? publicKey(response.body()) : null);
    } catch (IOException | InterruptedException | JSONException e) {
      ourLog.error(e.getMessage());
      return null;
    }
  }

  private static String publicKey(String body) throws JSONException {
    return new JSONObject(body).getString("public_key");
  }
}
