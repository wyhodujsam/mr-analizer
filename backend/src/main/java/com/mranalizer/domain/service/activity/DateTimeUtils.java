package com.mranalizer.domain.service.activity;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public final class DateTimeUtils {

    private static final int NIGHT_START = 22;
    private static final int NIGHT_END = 6;

    private DateTimeUtils() {}

    public static boolean isWeekend(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        DayOfWeek day = dateTime.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    public static boolean isNight(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        int hour = dateTime.getHour();
        return hour >= NIGHT_START || hour < NIGHT_END;
    }
}
