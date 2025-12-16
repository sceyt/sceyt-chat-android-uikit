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
        } catch (ex: Exception) {
            println("Error converting List<String>: ${ex.message}")
            emptyList()
        }
    }

    @TypeConverter
    fun objToString(obj: List<String>?): String? {
        obj ?: return null
        return stringListAdapter.toJson(obj)
    }

    // ---------- Map<String, Int> ----------

    @TypeConverter
    fun fromMap(value: Map<String, Int>?): String? {
        value ?: return null
        return stringIntMapAdapter.toJson(value)
    }

    @TypeConverter
    fun toMap(value: String?): Map<String, Int>? {
        value ?: return null
        return try {
            stringIntMapAdapter.fromJson(value)
        } catch (ex: Exception) {
            println("Error converting Map<String, Int>: ${ex.message}")
            emptyMap()
        }
    }
}