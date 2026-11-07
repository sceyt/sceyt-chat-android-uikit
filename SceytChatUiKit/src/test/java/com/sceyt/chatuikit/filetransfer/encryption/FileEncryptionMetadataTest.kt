package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import org.junit.Test

class FileEncryptionMetadataTest {
    @Test
    fun `metadata preserves the SDK token and existing media fields`() {
        val original = """{"tmb":"blur-thumbnail","szw":1280,"szh":203}"""
        val token = "opaque-sdk-token"

        val metadata = FileEncryptionMetadata.write(original, token)

        val json = JsonParser.parseString(metadata).asJsonObject
        assertThat(json.get("sceyt.enc").asString).isEqualTo(token)
        assertThat(json.get("tmb").asString).isEqualTo("blur-thumbnail")
        assertThat(json.get("szw").asInt).isEqualTo(1280)
        assertThat(json.get("szh").asInt).isEqualTo(203)
        assertThat(FileEncryptionMetadata.read(metadata)).isEqualTo(token)
        assertThat(JsonParser.parseString(FileEncryptionMetadata.remove(metadata)))
            .isEqualTo(JsonParser.parseString(original))
    }

    @Test
    fun `malformed metadata does not escape the parser`() {
        assertThat(FileEncryptionMetadata.read("{invalid-json")).isNull()
        assertThat(FileEncryptionMetadata.read("""{"sceyt.enc":{}}""")).isNull()
        assertThat(FileEncryptionMetadata.read(null)).isNull()
        assertThat(FileEncryptionMetadata.isEncrypted("""{"sceyt.enc":"invalid"}""")).isTrue()
    }

    @Test
    fun `key bearing models omit keys from diagnostic strings`() {
        val checkpoint = FileEncryptionCheckpoint("private-token", "private-checkpoint-state")

        assertThat(checkpoint.toString()).doesNotContain(checkpoint.token)
        assertThat(checkpoint.toString()).doesNotContain(checkpoint.data)
    }
}
