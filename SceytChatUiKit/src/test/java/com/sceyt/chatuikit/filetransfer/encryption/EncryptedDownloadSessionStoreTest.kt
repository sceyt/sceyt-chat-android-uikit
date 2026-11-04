package com.sceyt.chatuikit.filetransfer.encryption

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class EncryptedDownloadSessionStoreTest {
    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `a fresh store recovers the destination and committed plaintext offset`() {
        val directory = temporary.newFolder()
        val session =
            EncryptedDownloadSession("source-identity", temporary.newFile().path, 1_048_576)
        EncryptedDownloadSessionStore(directory).save("download:42", session)

        assertThat(EncryptedDownloadSessionStore(directory).get("download:42")).isEqualTo(session)
        assertThat(EncryptedDownloadSessionStore(directory).get("download:43")).isNull()
    }

    @Test
    fun `discard after recreation removes staging and preserves published plaintext`() {
        val directory = temporary.newFolder()
        val destination = temporary.newFile().apply { writeText("verified plaintext") }
        val staging = listOf(".sceyt.part", ".sceyt.enc", ".sceyt.enc.part").map {
            File(destination.path + it).apply { writeText("temporary") }
        }
        EncryptedDownloadSessionStore(directory).save(
            "download:42", EncryptedDownloadSession("identity", destination.path),
        )
        val restored = EncryptedDownloadSessionStore(directory)

        restored.discard("download:42")
        restored.discard("download:42")

        assertThat(staging.all { !it.exists() }).isTrue()
        assertThat(restored.get("download:42")).isNull()
        assertThat(destination.readText()).isEqualTo("verified plaintext")
    }

    @Test
    fun `partial deletion preserves the record and can be retried after recreation`() {
        val directory = temporary.newFolder()
        val destination = temporary.newFile().apply { writeText("published") }
        val blocked = File(destination.path + ".sceyt.enc").apply { mkdir() }
        val child = File(blocked, "still-in-use").apply { writeText("blocking deletion") }
        val otherStaging =
            File(destination.path + ".sceyt.enc.part").apply { writeText("temporary") }
        val store = EncryptedDownloadSessionStore(directory)
        store.save("download:42", EncryptedDownloadSession("identity", destination.path))

        assertThat(runCatching { store.discard("download:42") }.exceptionOrNull())
            .isInstanceOf(IOException::class.java)
        assertThat(EncryptedDownloadSessionStore(directory).get("download:42")?.discardRequested)
            .isTrue()
        assertThat(otherStaging.exists()).isFalse()
        assertThat(destination.readText()).isEqualTo("published")

        child.delete()
        EncryptedDownloadSessionStore(directory).discard("download:42")

        assertThat(blocked.exists()).isFalse()
        assertThat(store.get("download:42")).isNull()
        assertThat(destination.readText()).isEqualTo("published")
    }

    @Test
    fun `truncated JSON cannot become a usable resume record`() {
        val directory = temporary.newFolder()
        val store = EncryptedDownloadSessionStore(directory)
        store.save("download:42", EncryptedDownloadSession("identity", temporary.newFile().path))
        directory.listFiles().orEmpty().single().writeText("{broken")

        assertThat(store.get("download:42")).isNull()
        assertThat(directory.listFiles().orEmpty()).isEmpty()
    }
}
