package com.cs565project.smart.db;

import androidx.room.TypeConverter;

import java.util.Date;

/**
 * Converters required for Room database.
 */

@SuppressWarnings("unused")
public class Converters {
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
}
