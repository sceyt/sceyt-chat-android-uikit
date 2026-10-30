package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.fileencryption.FileEncryptionData
import com.sceyt.chat.fileencryption.FileEncryptionInfo
import com.sceyt.chat.fileencryption.StreamingAeadFileEncryptionProvider
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileUploadSessionStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `malformed session is discarded without affecting another upload and can be replaced`() {
        val store = FileUploadSessionStore(temporaryFolder.root)
        val broken = session("upload:broken", "old-source")
        store.save(broken)
        val record = temporaryFolder.root.listFiles()!!.single()
        record.writeText("{invalid-json")
        val healthy = session("upload:healthy", "healthy-source")
        store.save(healthy)

        assertThat(store.get(broken.operationId)).isNull()
        assertThat(record.exists()).isFalse()
        assertThat(store.get(healthy.operationId)).isEqualTo(healthy)

        val fresh = session(broken.operationId, "fresh-source")
        store.save(fresh)

        assertThat(store.get(fresh.operationId)).isEqualTo(fresh)
    }

    private fun session(operationId: String, sourceId: String) = UploadSession(
        operationId = operationId,
        sourceFilePath = "/tmp/upload.bin",
        originalFilePath = "/tmp/upload.bin",
        sourceId = sourceId,
        encryptionInfo = FileEncryptionInfo(
            StreamingAeadFileEncryptionProvider.FORMAT,
            FileEncryptionData(sourceId, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 32L),
        ),
        sdkCheckpointData = "sdk-checkpoint",
        transportCheckpointData = "backend-checkpoint",
    )
}
