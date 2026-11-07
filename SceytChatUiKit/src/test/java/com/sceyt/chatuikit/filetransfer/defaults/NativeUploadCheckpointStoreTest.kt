package com.sceyt.chatuikit.filetransfer.defaults

import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.filetransfer.encryption.FileEncryptionCheckpoint
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NativeUploadCheckpointStoreTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `a new store restores recovery data without persisting the token`() {
        val token = "private-sdk-token"
        NativeUploadCheckpointStore { temporary.root }.save(
            "upload:1", FileEncryptionCheckpoint(token, ByteArray(72), "native-recovery-data")
        )

        val restored = NativeUploadCheckpointStore { temporary.root }.read("upload:1", token)

        assertThat(restored?.attachmentKey).isEqualTo(token)
        assertThat(restored?.header).isEqualTo(ByteArray(72))
        assertThat(restored?.data).isEqualTo("native-recovery-data")
        assertThat(temporary.root.listFiles()!!.single().readText()).doesNotContain(token)
    }

    @Test
    fun `recovery data cannot be restored with a different token`() {
        val store = NativeUploadCheckpointStore { temporary.root }
        store.save("upload:1", FileEncryptionCheckpoint("first-token", ByteArray(72), "first-data"))

        assertThat(store.read("upload:1", "other-token")).isNull()
        store.save("upload:1", FileEncryptionCheckpoint("other-token", ByteArray(72), "other-data"))

        assertThat(store.read("upload:1", "first-token")).isNull()
        assertThat(store.read("upload:1", "other-token")?.data).isEqualTo("other-data")
    }

    @Test
    fun `late checkpoint updates cannot recreate an abandoned upload`() {
        val store = NativeUploadCheckpointStore { temporary.root }
        store.save("upload:1", FileEncryptionCheckpoint("token", ByteArray(72), "before-read"))
        store.update("upload:1", FileEncryptionCheckpoint("token", ByteArray(72), "after-read"))
        assertThat(store.read("upload:1", "token")?.data).isEqualTo("after-read")

        store.remove("upload:1")
        store.update("upload:1", FileEncryptionCheckpoint("token", ByteArray(72), "late-snapshot"))

        assertThat(store.read("upload:1", "token")).isNull()
        assertThat(temporary.root.listFiles()).isEmpty()
    }

    @Test
    fun `removing an upload keeps other recovery records`() {
        val store = NativeUploadCheckpointStore { temporary.root }
        store.save("upload:1", FileEncryptionCheckpoint("first-token", ByteArray(72), "first-data"))
        store.save("upload:2", FileEncryptionCheckpoint("second-token", ByteArray(72), "second-data"))

        store.remove("upload:1")
        store.remove("upload:1")

        assertThat(store.read("upload:1", "first-token")).isNull()
        assertThat(store.read("upload:2", "second-token")?.data).isEqualTo("second-data")
    }
}
