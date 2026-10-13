package com.sceyt.chatuikit.extensions

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MediaUriExtensionsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `local path with colon in file name gets file scheme and keeps full path`() {
        val path = "/data/user/0/app/files/copied_files/2026-10-03 13:46:40_8396c32cdcf95549_0.mp4"

        val uri = path.toMediaUri()

        assertThat(uri.scheme).isEqualTo("file")
        assertThat(uri.path).isEqualTo(path)
    }

    @Test
    fun `downloaded attachment path with colon in file name gets file scheme and keeps full path`() {
        val path = "/data/user/0/app/files/sceyt_videos/1759484800000/2026-10-03 13:46:40_8396c32cdcf95549_0.mp4"

        val uri = path.toMediaUri()

        assertThat(uri.scheme).isEqualTo("file")
        assertThat(uri.path).isEqualTo(path)
    }

    @Test
    fun `local path with hash in file name keeps full path`() {
        val path = "/data/user/0/app/files/copied_files/Clip #2.mp4"

        val uri = path.toMediaUri()

        assertThat(uri.scheme).isEqualTo("file")
        assertThat(uri.path).isEqualTo(path)
    }

    @Test
    fun `plain local path gets file scheme`() {
        val path = "/data/user/0/app/files/copied_files/video.mp4"

        val uri = path.toMediaUri()

        assertThat(uri.scheme).isEqualTo("file")
        assertThat(uri.path).isEqualTo(path)
    }

    @Test
    fun `remote url is parsed as is`() {
        val url = "https://cdn.example.com/media/video.mp4?token=a:b"

        assertThat(url.toMediaUri()).isEqualTo(Uri.parse(url))
    }

    @Test
    fun `content uri is parsed as is`() {
        val uri = "content://media/external/video/media/42"

        assertThat(uri.toMediaUri()).isEqualTo(Uri.parse(uri))
    }

    @Test
    fun `file uri string is parsed as is`() {
        val uri = "file:///data/user/0/app/files/video.mp4"

        assertThat(uri.toMediaUri()).isEqualTo(Uri.parse(uri))
    }

    @OptIn(UnstableApi::class)
    @Test
    fun `exoplayer data source reads local file with colon in name`() {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val file = File(tempFolder.newFolder("copied_files"), "2026-10-03 13:46:40_8396c32cdcf95549_0.mp4")
        file.writeBytes(bytes)
        val dataSource = DefaultDataSource.Factory(RuntimeEnvironment.getApplication()).createDataSource()

        val length = dataSource.open(DataSpec(file.absolutePath.toMediaUri()))
        val buffer = ByteArray(bytes.size)
        dataSource.read(buffer, 0, buffer.size)
        dataSource.close()

        assertThat(length).isEqualTo(bytes.size.toLong())
        assertThat(buffer).isEqualTo(bytes)
    }
}
