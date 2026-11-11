package com.sceyt.chatuikit.filetransfer.encryption

import com.google.gson.Gson
import com.google.gson.JsonObject

/** The default provider's metadata key and SDK attachment key. Custom providers own their own keys. */
object FileEncryptionMetadata {
    internal const val FIELD = "sceyt.enc"
    private val gson = Gson()

    fun isEncrypted(metadata: String?): Boolean = parse(metadata)?.has(FIELD) == true

    fun read(metadata: String?): String? = runCatching {
        parse(metadata)?.get(FIELD)?.asString
    }.getOrNull()

    fun write(metadata: String?, attachmentKey: String): String {
        val json = parse(metadata) ?: JsonObject()
        json.addProperty(FIELD, attachmentKey)
        return gson.toJson(json)
    }

    fun remove(metadata: String?): String? {
        val json = parse(metadata) ?: return metadata
        if (json.remove(FIELD) == null) return metadata
        return if (json.size() == 0) null else gson.toJson(json)
    }

    private fun parse(metadata: String?): JsonObject? = runCatching {
        gson.fromJson(metadata, JsonObject::class.java)
    }.getOrNull()
}
