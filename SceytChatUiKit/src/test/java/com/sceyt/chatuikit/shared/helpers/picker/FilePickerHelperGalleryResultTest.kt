package com.sceyt.chatuikit.shared.helpers.picker

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ContentProvider
import android.content.Intent
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FilePickerHelperGalleryResultTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `copies a returned photo and delivers its local path`() = runTest(timeout = 5.seconds) {
        Robolectric.buildActivity(GalleryActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val photo = PickedMedia("photo.jpg", byteArrayOf(1, 2, 3))
            val uri = activity.registerMedia(photo).single()

            val files = activity.receiveMedia(Intent().setData(uri), allowMultiple = false, onlyImages = true)

            assertThat(files).hasSize(1)
            assertCopiedFile(files.single(), photo, activity.filesDir)
        }
    }

    @Test
    fun `copies and returns all clip data items in selection order`() = runTest(timeout = 5.seconds) {
        Robolectric.buildActivity(GalleryActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val photo = PickedMedia("photo.jpg", byteArrayOf(1, 2, 3))
            val video = PickedMedia("video.mp4", byteArrayOf(4, 5, 6))
            val (photoUri, videoUri) = activity.registerMedia(photo, video)
            val result = Intent().apply {
                clipData = ClipData.newRawUri("Selected media", videoUri).apply {
                    addItem(ClipData.Item(photoUri))
                }
            }

            val files = activity.receiveMedia(result, allowMultiple = true, onlyImages = false)

            assertThat(files).hasSize(2)
            assertCopiedFile(files[0], video, activity.filesDir)
            assertCopiedFile(files[1], photo, activity.filesDir)
        }
    }

    @Test
    fun `cancelling gallery selection does not request a destination or call back`() = runTest {
        Robolectric.buildActivity(GalleryActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            var destinationRequested = false
            var callbackCalled = false
            activity.picker.chooseFromGallery(
                allowMultiple = false,
                onlyImages = true,
                parentDirToCopyProvider = {
                    destinationRequested = true
                    activity.filesDir
                },
            ) {
                callbackCalled = true
            }
            val request = requireNotNull(shadowOf(activity).nextStartedActivityForResult)

            assertThat(activity.activityResultRegistry.dispatchResult(request.requestCode, RESULT_CANCELED, null))
                .isTrue()
            runCurrent()

            assertThat(destinationRequested).isFalse()
            assertThat(callbackCalled).isFalse()
        }
    }

    private suspend fun GalleryActivity.receiveMedia(
        result: Intent,
        allowMultiple: Boolean,
        onlyImages: Boolean,
    ): List<File> {
        val copiedPaths = CompletableDeferred<List<String>>()
        picker.chooseFromGallery(allowMultiple, onlyImages, parentDirToCopyProvider = { filesDir }) {
            copiedPaths.complete(it)
        }
        val request = requireNotNull(shadowOf(this).nextStartedActivityForResult)
        assertThat(activityResultRegistry.dispatchResult(request.requestCode, RESULT_OK, result)).isTrue()
        return copiedPaths.await().map(::File)
    }

    private fun GalleryActivity.registerMedia(vararg items: PickedMedia): List<Uri> {
        val authority = "test.gallery.provider"
        val provider = mock<ContentProvider>()
        ShadowContentResolver.registerProviderInternal(authority, provider)
        val resolver = shadowOf(contentResolver)
        return items.mapIndexed { index, media ->
            val uri = Uri.parse("content://$authority/picker/$index")
            whenever(provider.query(eq(uri), any(), isNull(), isNull(), isNull())).thenAnswer {
                MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME)).apply { addRow(arrayOf(media.name)) }
            }
            resolver.registerInputStreamSupplier(uri) { media.bytes.inputStream() }
            uri
        }
    }

    private fun assertCopiedFile(file: File, media: PickedMedia, parent: File) {
        assertThat(file.toPath().startsWith(parent.toPath())).isTrue()
        assertThat(file.name).isEqualTo(media.name)
        assertThat(file.readBytes()).isEqualTo(media.bytes)
    }

    @Suppress("ArrayInDataClass")
    private data class PickedMedia(val name: String, val bytes: ByteArray)

    class GalleryActivity : ComponentActivity() {
        val picker = FilePickerHelper(this)

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.SceytAppTheme)
            super.onCreate(savedInstanceState)
        }
    }
}
