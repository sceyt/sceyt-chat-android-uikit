package com.sceyt.chatuikit.filetransfer.encryption

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.sceyt.chat.fileencryption.FileEncryptionData
import com.sceyt.chat.fileencryption.FileEncryptionInfo
import com.sceyt.chat.fileencryption.StreamingAeadFileEncryptionProvider

/** Attachment metadata `enc: {id, v, k}`; the plaintext size is the attachment's file size. */
data class FileEncryptionMetadata(
    @SerializedName("id") val uniqueId: String,
    @SerializedName("v") val version: Int,
    @SerializedName("k") val key: String,
) {
    fun toEncryptionInfo(plainSize: Long): FileEncryptionInfo = FileEncryptionInfo(
        StreamingAeadFileEncryptionProvider.FORMAT,
        FileEncryptionData(uniqueId, key, plainSize),
    )

    companion object {
        private val gson = Gson()

        fun fromEncryptionInfo(info: FileEncryptionInfo): FileEncryptionMetadata =
            FileEncryptionMetadata(info.data.uniqueId, 1, info.data.key)

        fun isEncrypted(metadata: String?): Boolean = parse(metadata)?.has("enc") == true

        fun read(metadata: String?): FileEncryptionMetadata? = runCatching {
            gson.fromJson(parse(metadata)?.get("enc"), FileEncryptionMetadata::class.java)
        }.getOrNull()

        fun write(metadata: String?, envelope: FileEncryptionMetadata): String {
            val json = parse(metadata) ?: JsonObject()
            json.add("enc", gson.toJsonTree(envelope))
            return gson.toJson(json)
        }

        fun remove(metadata: String?): String? {
            val json = parse(metadata) ?: return metadata
            if (json.remove("enc") == null) return metadata
            return if (json.size() == 0) null else gson.toJson(json)
        }

        private fun parse(metadata: String?): JsonObject? = runCatching {
            gson.fromJson(metadata, JsonObject::class.java)
        }.getOrNull()
    }
}
