package com.sceyt.chatuikit.persistence.file_transfer

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.filetransfer.encryption.ChatSdkFileEncryption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ChatSdkFileEncryptionAndroidTest {
    @Test
    fun generatedAttachmentIdsWorkWithNativePreparationAndRestore() = runBlocking(Dispatchers.IO) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File.createTempFile("attachment-id", ".bin", context.cacheDir)
        try {
            file.writeBytes(ByteArray(64 * 1024 + 1) { (it % 251).toByte() })
            val encryption = ChatSdkFileEncryption()
            val (checkpoint, ciphertext) = encryption.prepare(file).use { session ->
                val id = session.encryptionInfo.attachmentId
                assertThat(id).matches("[A-Za-z0-9_-]{22}")
                assertThat(Base64.decode(id, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)).hasLength(16)
                val bytes = session.read(0, session.ciphertextSize.toInt())
                session.checkpoint() to bytes
            }

            encryption.restore(file, checkpoint).use { restored ->
                assertThat(restored.encryptionInfo.attachmentId).isEqualTo(checkpoint.encryptionInfo.attachmentId)
                assertThat(restored.read(0, ciphertext.size)).isEqualTo(ciphertext)
            }
            encryption.prepare(file).use { next ->
                assertThat(next.encryptionInfo.attachmentId).isNotEqualTo(checkpoint.encryptionInfo.attachmentId)
            }
        } finally {
            file.delete()
        }
    }
}
