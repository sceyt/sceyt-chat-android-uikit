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
            "upload:1", FileEncryptionCheckpoint(token, "native-recovery-data")
        )

        val restored = NativeUploadCheckpointStore { temporary.root }.read("upload:1", token)

        assertThat(restored?.token).isEqualTo(token)
        assertThat(restored?.data).isEqualTo("native-recovery-data")
        assertThat(temporary.root.listFiles()!!.single().readText()).doesNotContain(token)
    }

    @Test
    fun `recovery data cannot be restored with a different token`() {
        val store = NativeUploadCheckpointStore { temporary.root }
        store.save("upload:1", FileEncryptionCheckpoint("first-token", "first-data"))

        assertThat(store.read("upload:1", "other-token")).isNull()
        store.save("upload:1", FileEncryptionCheckpoint("other-token", "other-data"))

        assertThat(store.read("upload:1", "first-token")).isNull()
        assertThat(store.read("upload:1", "other-token")?.data).isEqualTo("other-data")
    }

    @Test
    fun `late checkpoint updates cannot recreate an abandoned upload`() {
        val store = NativeUploadCheckpointStore { temporary.root }
        store.save("upload:1", FileEncryptionCheckpoint("token", "before-read"))
        store.update("upload:1", FileEncryptionCheckpoint("token", "after-read"))
        assertThat(store.read("upload:1", "token")?.data).isEqualTo("after-read")

        store.remove("upload:1")
        store.update("upload:1", FileEncryptionCheckpoint("token", "late-snapshot"))

        assertThat(store.read("upload:1", "token")).isNull()
        assertThat(temporary.root.listFiles()).isEmpty()
    }

    @Test
    fun `removing an upload keeps other recovery records`() {
        val store = NativeUploadCheckpointStore { temporary.root }
        store.save("upload:1", FileEncryptionCheckpoint("first-token", "first-data"))
        store.save("upload:2", FileEncryptionCheckpoint("second-token", "second-data"))

        store.remove("upload:1")
        store.remove("upload:1")

        assertThat(store.read("upload:1", "first-token")).isNull()
        assertThat(store.read("upload:2", "second-token")?.data).isEqualTo("second-data")
    }
}
