package com.multisectionbrowser.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Long {
        return value ?: 0L
    }

    @TypeConverter
    fun toTimestamp(value: Long): Long {
        return value
    }
}