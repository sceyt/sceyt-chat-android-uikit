package com.sceyt.chatuikit.extensions

import org.json.JSONObject

fun JSONObject.getStringOrNull(key: String): String? {
    return try {
        getString(key)
    } catch (e: Exception) {
        null
    }
}


fun JSONObject.getIntOrNull(key: String): Int? {
    return try {
        getInt(key)
    } catch (e: Exception) {
        null
    }
}

fun JSONObject.getLongOrNull(key: String): Long? {
    return try {
        getLong(key)
    } catch (e: Exception) {
        null
    }
}

fun JSONObject.getBooleanOrNull(key: String): Boolean? {
    return try {
        getBoolean(key)
    } catch (e: Exception) {
        null
    }
}