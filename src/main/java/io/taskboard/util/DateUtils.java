package io.taskboard.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

/**
 * Created by akiraabe on 2017/05/01.
 */
public class DateUtils {

    /**
     * インスタンス化抑止のためのprivateコンストラクタです。
     */
    private DateUtils() {}

    /**
     * Parse String to Date.
     * (Using Java8 java.time....)
     *
     * @param date "yyyy-MM-dd" format
     * @return java.util.Date
     */
    public static Date parse(String date) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        TemporalAccessor ta = fmt.parse(date);
        ZonedDateTime zdt = LocalDate.from(ta).atTime(0, 0).atZone(ZoneId.of("Asia/Tokyo"));
        return  Date.from(zdt.toInstant());
    }
}
