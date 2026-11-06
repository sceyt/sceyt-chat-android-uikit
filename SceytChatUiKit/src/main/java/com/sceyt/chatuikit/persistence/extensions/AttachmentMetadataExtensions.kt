package com.sceyt.chatuikit.persistence.extensions

import com.google.gson.Gson
import com.google.gson.JsonObject

private val metadataGson = Gson()
private const val FILE_TRANSFER_FIELD = "file_transfer"

private fun String?.toMetadataJson(): JsonObject =
    metadataGson.fromJson(this, JsonObject::class.java) ?: JsonObject()

/** Reads the app's data; existing non-JSON metadata has no file_transfer object. */
internal fun String?.fileTransferData(): JsonObject? =
    runCatching { toMetadataJson() }.getOrNull()?.getAsJsonObject(FILE_TRANSFER_FIELD)

/** Stores the app's object unchanged, preserving the rest of the attachment metadata. */
internal fun String?.withFileTransferData(data: JsonObject): String {
    val metadata = toMetadataJson()
    metadata.add(FILE_TRANSFER_FIELD, data.deepCopy())
    return metadata.toString()
}
