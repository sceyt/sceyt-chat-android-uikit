package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import com.sceyt.chat.fileencryption.FileEncryptionData
import com.sceyt.chat.fileencryption.FileEncryptionInfo
import com.sceyt.chat.fileencryption.StreamingAeadFileEncryptionProvider
import org.junit.Test

class FileEncryptionMetadataTest {
    @Test
    fun `enc stores only id version and the raw key while preserving media metadata`() {
        val original = """{"tmb":"ChgWCYZ4eHiIiAiHiIiAhwg=","szw":1280,"szh":203}"""
        val info = FileEncryptionInfo(
            StreamingAeadFileEncryptionProvider.FORMAT,
            FileEncryptionData(
                "4b61fd06-bdf7-43e2-bb3e-3a54f20a8a00",
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                5_368_709_120L,
            ),
        )
        val envelope = FileEncryptionMetadata.fromEncryptionInfo(info)

        val metadata = FileEncryptionMetadata.write(original, envelope)

        val json = JsonParser.parseString(metadata).asJsonObject
        val enc = json.getAsJsonObject("enc")
        assertThat(enc.keySet()).containsExactly("id", "v", "k")
        assertThat(enc.get("id").asString).isEqualTo(info.data.uniqueId)
        assertThat(enc.get("v").asInt).isEqualTo(1)
        assertThat(enc.get("k").asString).isEqualTo(info.data.key)
        assertThat(json.get("tmb").asString).isEqualTo("ChgWCYZ4eHiIiAiHiIiAhwg=")
        assertThat(json.get("szw").asInt).isEqualTo(1280)
        assertThat(json.get("szh").asInt).isEqualTo(203)
        assertThat(FileEncryptionMetadata.read(metadata)).isEqualTo(envelope)
        assertThat(checkNotNull(FileEncryptionMetadata.read(metadata)).toEncryptionInfo(info.data.plainSize))
            .isEqualTo(info)
        assertThat(JsonParser.parseString(FileEncryptionMetadata.remove(metadata)))
            .isEqualTo(JsonParser.parseString(original))
    }
}
