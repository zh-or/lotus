package or.lotus.core.common;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateUtils {
    private static ZoneId zoneId = ZoneId.of("+08:00");

    /** 需要修改时区时调用此方法 */
    public static void setZoneId(ZoneId zoneId) {
        DateUtils.zoneId = zoneId;
    }

    public static Date getDateFromStr(String pattern, String dateStr) {
        return new Date(getDateTimeFromStr(pattern, dateStr).toInstant().toEpochMilli());
    }

    public static ZonedDateTime getDateTimeFromStr(String pattern, String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return ZonedDateTime.parse(dateStr, formatter);
    }

    public static Date getLocalDate() {
        return new Date(getDate().toInstant().toEpochMilli());
    }

    public static ZonedDateTime getDate() {
        return ZonedDateTime.now(zoneId);
    }

    public static String getDateFormat(String pattern) {
        ZonedDateTime time = getDate();
        return time.format(DateTimeFormatter.ofPattern(pattern));
    }

    /** return 20241106 */
    public static int getFullYearDay() {
        String dateStr = getDateFormat("yyyyMMdd");
        return Integer.valueOf(dateStr);
    }

    /** yyyy-MM-dd HH:mm:ss*/
    public static String format(long time) {
        return format(time, "yyyy-MM-dd HH:mm:ss");
    }

    public static String format(long time, String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.format(new Date(time));
    }

}
