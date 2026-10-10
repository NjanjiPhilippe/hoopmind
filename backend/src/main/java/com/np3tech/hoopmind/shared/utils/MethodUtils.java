package com.np3tech.hoopmind.shared.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MethodUtils {
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static LocalDateTime convertorDateTime00H00(String dateStr) {
        if (isNullOrEmpty(dateStr)) {
            return null;
        }
        String dateTimeStr = dateStr.trim() + "T00:00:00";
        return LocalDateTime.parse(dateTimeStr);
    }
    public static LocalDateTime convertorDateTime24H00(String dateStr) {
        if (isNullOrEmpty(dateStr)) {
            return null;
        }
        String dateTimeStr = dateStr.trim() + "T24:00:00";
        return LocalDateTime.parse(dateTimeStr);
    }

    public static LocalDate convertorDate(String dateStr) {
        if (isNullOrEmpty(dateStr)) {
            return null;
        }
        return LocalDate.parse(dateStr.trim());
    }

}
