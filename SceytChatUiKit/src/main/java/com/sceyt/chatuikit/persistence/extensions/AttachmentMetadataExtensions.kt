package com.sceyt.chatuikit.persistence.extensions

import com.google.gson.Gson
import com.google.gson.JsonObject

private val metadataGson = Gson()

/** Merges the provider's string values, preserving unrelated attachment metadata. */
internal fun String?.withMetadata(data: Map<String, String>): String {
    val metadata = metadataGson.fromJson(this, JsonObject::class.java) ?: JsonObject()
    data.forEach { (key, value) -> metadata.addProperty(key, value) }
    return metadata.toString()
}

internal fun Map<String, String>.toMetadataJson(): String = metadataGson.toJson(this)

internal fun String.toMetadataMap(): Map<String, String> {
    val metadata = metadataGson.fromJson(this, JsonObject::class.java) ?: return emptyMap()
    return metadata.entrySet().associate { (key, value) -> key to value.asString }
}
