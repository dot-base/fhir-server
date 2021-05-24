package ca.uhn.fhir.jpa.starter.dotBase.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(
    DateUtils.class
  );

  public static String getCurrentTimestamp() {
    TimeZone localTimeZone = getTimeZone();
    String offSet = getOffSet_UTC(localTimeZone);
    SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss" + offSet);
    utcFormat.setTimeZone(localTimeZone);
    return utcFormat.format(new Date());
  }

  private static TimeZone getTimeZone() {
    if (TimeZone.getDefault() == null) return TimeZone.getTimeZone("UTC");
    return TimeZone.getDefault();
  }

  private static String getOffSet_UTC(TimeZone localTimeZone) {
    int rawOffSet = localTimeZone.getRawOffset() / 3600000 + 1;
    String operator = rawOffSet >= 0 ? "+" : "-";
    return operator + String.format("%02d", rawOffSet) + ":00";
  }
}
