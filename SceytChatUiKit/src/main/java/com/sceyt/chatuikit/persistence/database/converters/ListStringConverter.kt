package com.sceyt.chatuikit.persistence.database.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

object ListStringConverter {

    private val moshi: Moshi = Moshi.Builder().build()

    private val stringListAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )

    private val stringIntMapAdapter = moshi.adapter<Map<String, Int>>(
        Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            Int::class.javaObjectType
        )
    )

    // ---------- List<String> ----------

    @TypeConverter
    fun stringToObj(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            stringListAdapter.fromJson(json).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun objToString(obj: List<String>?): String? {
        if (obj.isNullOrEmpty()) return null
        return stringListAdapter.toJson(obj)
    }

    // ---------- Map<String, Int> ----------

    @TypeConverter
    fun fromMap(value: Map<String, Int>?): String? {
        if (value.isNullOrEmpty()) return null
        return stringIntMapAdapter.toJson(value)
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, Int>? {
        if (value.isNullOrEmpty()) return null
        return try {
            stringIntMapAdapter.fromJson(value)
        } catch (_: Exception) {
            null
        }
    }
}