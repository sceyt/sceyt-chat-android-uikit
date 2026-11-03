package com.sceyt.chatuikit.filetransfer.encryption

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/** Attachment encryption metadata; the plaintext size remains the attachment's file size. */
data class FileEncryptionMetadata(
    @SerializedName("v") val version: Int = 1,
    @SerializedName("alg") val algorithm: String,
    val attachmentId: String,
    val key: String,
    val renditions: List<FileEncryptionRendition> = emptyList(),
) {
    fun toEncryptionInfo(): FileEncryptionInfo = FileEncryptionInfo(algorithm, attachmentId, key)

    override fun toString(): String =
        "FileEncryptionMetadata(version=$version, algorithm=$algorithm, attachmentId=$attachmentId)"

    companion object {
        private val gson = Gson()

        fun fromEncryptionInfo(
            info: FileEncryptionInfo,
            renditions: List<FileEncryptionRendition> = emptyList(),
        ): FileEncryptionMetadata = FileEncryptionMetadata(
            algorithm = info.algorithm,
            attachmentId = info.attachmentId,
            key = info.key,
            renditions = renditions,
        )

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

data class FileEncryptionRendition(
    val role: String,
    val attachmentId: String,
    val key: String,
) {
    override fun toString(): String =
        "FileEncryptionRendition(role=$role, attachmentId=$attachmentId)"
}
