package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import org.junit.Test

class FileEncryptionMetadataTest {
    @Test
    fun `metadata preserves algorithm binding IDs independent rendition keys and media fields`() {
        val original = """{"tmb":"blur-thumbnail","szw":1280,"szh":203}"""
        val info = FileEncryptionInfo("returned-sdk-format", "main-id", "main-file-key")
        val thumbnail = FileEncryptionRendition("thumbnail", "thumbnail-id", "thumbnail-file-key")
        val envelope = FileEncryptionMetadata.fromEncryptionInfo(info, listOf(thumbnail))

        val metadata = FileEncryptionMetadata.write(original, envelope)

        val json = JsonParser.parseString(metadata).asJsonObject
        val enc = json.getAsJsonObject("enc")
        assertThat(enc.keySet()).containsExactly("v", "alg", "attachmentId", "key", "renditions")
        assertThat(enc.get("v").asInt).isEqualTo(1)
        assertThat(enc.get("alg").asString).isEqualTo(info.algorithm)
        assertThat(enc.get("attachmentId").asString).isEqualTo(info.attachmentId)
        assertThat(enc.get("key").asString).isEqualTo(info.key)
        val rendition = enc.getAsJsonArray("renditions").single().asJsonObject
        assertThat(rendition.keySet()).containsExactly("role", "attachmentId", "key")
        assertThat(rendition.get("key").asString).isEqualTo(thumbnail.key)
        assertThat(rendition.get("attachmentId").asString).isEqualTo(thumbnail.attachmentId)
        assertThat(json.get("tmb").asString).isEqualTo("blur-thumbnail")
        assertThat(json.get("szw").asInt).isEqualTo(1280)
        assertThat(json.get("szh").asInt).isEqualTo(203)
        val decoded = checkNotNull(FileEncryptionMetadata.read(metadata))
        assertThat(decoded).isEqualTo(envelope)
        assertThat(decoded.toEncryptionInfo()).isEqualTo(info)
        assertThat(JsonParser.parseString(FileEncryptionMetadata.remove(metadata)))
            .isEqualTo(JsonParser.parseString(original))
    }

    @Test
    fun `malformed metadata does not escape the parser`() {
        assertThat(FileEncryptionMetadata.read("{invalid-json")).isNull()
        assertThat(FileEncryptionMetadata.read("""{"enc":"invalid"}""")).isNull()
        assertThat(FileEncryptionMetadata.read(null)).isNull()
        assertThat(FileEncryptionMetadata.isEncrypted("""{"enc":"invalid"}""")).isTrue()
    }

    @Test
    fun `key bearing models omit keys from diagnostic strings`() {
        val info = FileEncryptionInfo("format", "main-id", "private-main-key")
        val rendition = FileEncryptionRendition("thumbnail", "thumb-id", "private-thumbnail-key")
        val envelope = FileEncryptionMetadata.fromEncryptionInfo(info, listOf(rendition))
        val checkpoint = FileEncryptionCheckpoint(info, 42L, "private-checkpoint-state")

        assertThat(info.toString()).doesNotContain(info.key)
        assertThat(rendition.toString()).doesNotContain(rendition.key)
        assertThat(envelope.toString()).doesNotContain(info.key)
        assertThat(envelope.toString()).doesNotContain(rendition.key)
        assertThat(checkpoint.toString()).doesNotContain(info.key)
        assertThat(checkpoint.toString()).doesNotContain(checkpoint.data)
    }
}
