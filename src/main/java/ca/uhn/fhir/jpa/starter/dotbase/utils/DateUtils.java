package ca.uhn.fhir.jpa.starter.dotbase.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtils {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(DateUtils.class);

  public static String getCurrentTimestamp() {
    TimeZone timeZone = getTimeZone();
    String offSet = getOffSet_UTC(timeZone);
    SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"+offSet);
    return utcFormat.format(new Date());
  }

  private static TimeZone getTimeZone() {
    if (TimeZone.getDefault() == null){
      return TimeZone.getTimeZone("UTC");
    }
    return TimeZone.getDefault();
  }

  private static String getOffSet_UTC(TimeZone localTimeZone) {
    Calendar cal = Calendar.getInstance();
    int offSet = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET))/ 3600000;
    String operator = offSet >= 0 ? "+" : "-";
    return operator + String.format("%02d", offSet) + ":00";
  }
}