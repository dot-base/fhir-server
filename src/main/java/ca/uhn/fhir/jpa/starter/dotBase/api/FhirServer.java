package ca.uhn.fhir.jpa.starter.dotBase.api;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.context.FhirContext;

import java.io.IOException;
import java.net.URI;

public class FhirServer {
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FhirServer.class);
    private static final HttpClient HTTP_CLIENT = FhirServer.clientConfig();
    private static final IParser r4Parser = FhirContext.forR4().newJsonParser();

    private static final HttpClient clientConfig() {
        return HttpClient.newBuilder().version(Version.HTTP_2).connectTimeout(Duration.ofSeconds(30))
                .followRedirects(Redirect.NEVER).build();
    }

    private static String getExternalResource(String uri) {
        uri = "https://vonk.fire.ly/Patient/MLD0500228";
        HttpResponse<String> response = null;
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri))
        .setHeader("Content-Type", "application/fhir+json")
        //.setHeader("Accept", "application/fhir+json; fhirVersion=4.0")
        .GET().build();
        try {
            response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            ourLog.info(e.getMessage());
        }
        return (response.statusCode()==200 ? response.body() : "");
    }

    public static Patient getPatient(String fullUrl) {
        String resource = getExternalResource(fullUrl);
        return FhirServer.r4Parser.parseResource(Patient.class, resource);
    }
}
