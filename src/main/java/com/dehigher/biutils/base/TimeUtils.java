package com.dehigher.biutils.base;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimeUtils {


    public static long time2stamp(String formatter, String timestr) {
        LocalDateTime localDateTime = LocalDateTime.parse(timestr, DateTimeFormatter.ofPattern(formatter));
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        long timestamp = instant.toEpochMilli();
        return timestamp;
    }

    public static String format(long timestampInMillis) {
        // 将毫秒数转换为 Instant
        Instant instant = Instant.ofEpochMilli(timestampInMillis);
        // 将时间戳转换为 LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        // 定义日期时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 格式化日期时间
        return localDateTime.format(formatter);
    }
}
