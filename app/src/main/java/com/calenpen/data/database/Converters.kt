package com.calenpen.data.database

import androidx.room.TypeConverter

/** Room type converters for types that cannot be stored directly in SQLite. */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        value?.joinToString(separator = "|||")

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        value?.split("|||")?.filter { it.isNotEmpty() }
}
