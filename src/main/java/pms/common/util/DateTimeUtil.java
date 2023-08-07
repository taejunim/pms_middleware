package pms.common.util;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

/**
 * Date Time Utility
 * <p>
 * - 일자 시간 관련 함수 유틸리티
 */
public class DateTimeUtil {
    /**
     * 현재 DateTimestamp 호출
     * <p>
     * - 'yyyy-MM-dd HH:mm:ss' 형식
     *
     * @return 'yyyy-MM-dd HH:mm:ss' 형식의 현재 DateTimestamp
     */
    public static String getCurrentTimestamp() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return dateFormat.format(currentDate);
    }

    /**
     * 원하는 포맷에 맞게 현재 DateTimestamp 호출
     *
     * @param format 포맷 형식
     * @return 포맷 형식의 현재 DateTimestamp
     */
    public static String getDateFormat(String format) {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);

        return dateFormat.format(currentDate);
    }

    /**
     * 현재 Unix Timestamp 호출
     * <p>
     * - Unix Time 형식으로 호출
     *
     * @return Unix Time 형식의 현재 Timestamp
     */
    public static int getUnixTimestamp() {
        long unixTime = Instant.now().getEpochSecond();

        return (int) unixTime;
    }

    public static Date getCalculatedDate(int field, int amount) {
        Date currentDate = new Date();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(field, amount);

        Date calculatedDate = new Date(calendar.getTimeInMillis());

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println(dateFormat.format(calculatedDate));

        return calculatedDate;
    }

    public static Date getCalculatedDate(String dateString, int field, int amount) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        Date currentDate = null;
        try {
            currentDate = dateFormat.parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(field, amount);

        Date calculatedDate = new Date(calendar.getTimeInMillis());

        return calculatedDate;
    }
}
