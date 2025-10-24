package com.sceyt.chatuikit.persistence.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ListStringConverter {
    @TypeConverter
    fun stringToObj(json: String?): List<String> {
        json ?: return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }

    @TypeConverter
    fun objToString(obj: List<String>?): String? {
        if (obj == null)
            return null

        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.toJson(obj, type)
    }

    @TypeConverter
    fun fromMap(value: Map<String, Int>?): String? {
        return value?.let { Gson().toJson(it) }
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, Int>? {
        return value?.let {
            val mapType = object : TypeToken<Map<String, Int>>() {}.type
            Gson().fromJson(it, mapType)
        }
    }
}