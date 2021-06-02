package ca.uhn.fhir.jpa.starter.dotBase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DotbaseProperties {

  public static String get(String propertyname) {
    InputStream input =
      DotbaseProperties.class.getClassLoader().getResourceAsStream("dotbase.properties");
    Properties prop = new Properties();
    try {
      prop.load(input);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new Properties().getProperty(propertyname);
  }
}
