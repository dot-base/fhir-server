package ca.uhn.fhir.jpa.starter.dotBase.services;

import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Authentication {
  private static final org.slf4j.Logger OUR_LOG = org.slf4j.LoggerFactory.getLogger(
    Authentication.class
  );
  private static final String REALM_PUBLIC_KEY = HapiProperties
    .getJpaProperties()
    .getProperty("REALM_PUBLIC_KEY");

  public static Claims verifyAndDecodeJWT(RequestDetails theRequestDetails) {
    try {
      PublicKey key = decodePublicKey(pemToDer(REALM_PUBLIC_KEY));
      String authToken = getAuthToken(theRequestDetails);
      Claims claims = Jwts
        .parser()
        .setSigningKey(key)
        .parseClaimsJws(authToken)
        .getBody();
      return claims;
    } catch (Exception e) {
      throw new AuthenticationException("Authentication failed.");
    }
  }

  private static String getAuthToken(RequestDetails theRequestDetails) {
    String authHeader = theRequestDetails.getHeader("Authorization");
    if (authHeader != null) {
      return getBearerToken(authHeader);
    }
    throw new AuthenticationException("Request must include authorization header.");
  }

  private static String getBearerToken(String authHeader) {
    String[] splitToken = authHeader.split("[Bb]earer ");
    if (splitToken.length != 2) throw new AuthenticationException(
      "Invalid bearer token format."
    );
    return splitToken[1];
  }

  private static byte[] pemToDer(String pem) {
    return Base64.getDecoder().decode(stripBeginEnd(pem));
  }

  private static String stripBeginEnd(String pem) {
    String stripped = pem.replaceAll("-----BEGIN (.*)-----", "");
    stripped = stripped.replaceAll("-----END (.*)----", "");
    stripped = stripped.replaceAll("\r\n", "");
    stripped = stripped.replaceAll("\n", "");
    return stripped.trim();
  }

  private static PublicKey decodePublicKey(byte[] der)
    throws InvalidKeySpecException, NoSuchAlgorithmException {
    X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    return keyFactory.generatePublic(spec);
  }
}
