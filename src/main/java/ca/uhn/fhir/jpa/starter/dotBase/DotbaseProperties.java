package ca.uhn.fhir.jpa.starter.dotBase;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DotbaseProperties {

  public static String get(String propertyname) {
    Properties prop = new Properties();
    try {
      InputStream input =
        DotbaseProperties.class.getClassLoader().getResourceAsStream("dotbase.properties");
      prop.load(input);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return prop.getProperty(propertyname);
  }
}
