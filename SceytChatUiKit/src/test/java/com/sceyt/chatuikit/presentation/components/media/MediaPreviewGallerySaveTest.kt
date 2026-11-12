package com.sceyt.chatuikit.presentation.components.media

import android.Manifest
import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.system.ErrnoException
import android.system.OsConstants
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.presentation.common.dialogs.SceytDialog
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItem
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowToast
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

@RunWith(RobolectricTestRunner::class)
class MediaPreviewGallerySaveTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    @Config(sdk = [29, 33])
    fun `save starts without requesting storage permission on Android 10 and later`() {
        val activity = Robolectric.buildActivity(GallerySaveMediaPreviewActivity::class.java).setup().get()
        val item = mock<MediaItem>()

        activity.tapSave(item)

        assertThat(activity.savedItem).isSameInstanceAs(item)
        assertThat(shadowOf(activity).lastRequestedPermission).isNull()
    }

    @Test
    @Config(sdk = [28])
    fun `save waits for write permission on Android 9`() {
        val activity = Robolectric.buildActivity(GallerySaveMediaPreviewActivity::class.java).setup().get()
        val item = mock<MediaItem>()

        activity.tapSave(item)

        assertThat(activity.savedItem).isNull()
        val request = shadowOf(activity).lastRequestedPermission
        assertThat(request.requestedPermissions.asList())
            .containsExactly(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            intArrayOf(PackageManager.PERMISSION_GRANTED),
        )
        assertThat(activity.savedItem).isSameInstanceAs(item)
    }

    @Test
    @Config(sdk = [29])
    fun `save publishes copied image bytes in the pictures collection`() {
        val context = RuntimeEnvironment.getApplication()
        val resolver = shadowOf(context.contentResolver)
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val destination = ContentUris.withAppendedId(collection, 1)
        val source = tempFolder.newFile("photo.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val output = ByteArrayOutputStream()
        val provider = mock<ContentProvider>()
        ShadowContentResolver.registerProviderInternal(collection.authority, provider)
        whenever(provider.insert(eq(collection), any())).thenReturn(destination)
        whenever(provider.update(eq(destination), any(), isNull(), isNull())).thenReturn(1)
        resolver.registerOutputStream(destination, output)

        val result = MediaGallerySaver.save(context, source.path, source.name, "image/png")

        assertThat(result).isEqualTo(GallerySaveResult.Saved(destination))
        assertThat(output.toByteArray()).isEqualTo(source.readBytes())
        val values = argumentCaptor<ContentValues>()
        verify(provider).insert(eq(collection), values.capture())
        assertThat(values.lastValue.getAsString(MediaStore.MediaColumns.RELATIVE_PATH))
            .isEqualTo(Environment.DIRECTORY_PICTURES)
        assertThat(values.lastValue.getAsInteger(MediaStore.MediaColumns.IS_PENDING)).isEqualTo(1)
        verify(provider).update(eq(destination), values.capture(), isNull(), isNull())
        assertThat(values.lastValue.getAsInteger(MediaStore.MediaColumns.IS_PENDING)).isEqualTo(0)
    }

    @Test
    @Config(sdk = [33])
    fun `save removes the pending gallery entry when the copy fails`() {
        val context = RuntimeEnvironment.getApplication()
        val resolver = shadowOf(context.contentResolver)
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val destination = ContentUris.withAppendedId(collection, 1)
        val source = tempFolder.newFile("video.mp4").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val provider = mock<ContentProvider>()
        ShadowContentResolver.registerProviderInternal(collection.authority, provider)
        whenever(provider.insert(eq(collection), any())).thenReturn(destination)
        resolver.registerOutputStream(destination, object : OutputStream() {
            override fun write(value: Int) = throw IOException("Copy failed")
        })

        val result = MediaGallerySaver.save(context, source.path, source.name, "video/mp4")

        assertThat(result).isEqualTo(GallerySaveResult.Failed)
        verify(provider).delete(destination, null, null)
        verify(provider, never()).update(eq(destination), any(), isNull(), isNull())
    }

    @Test
    @Config(sdk = [33])
    fun `save shows storage full dialog when the device runs out of space`() {
        val context = RuntimeEnvironment.getApplication()
        val resolver = shadowOf(context.contentResolver)
        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val destination = ContentUris.withAppendedId(collection, 1)
        val source = tempFolder.newFile("video.mp4").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val provider = mock<ContentProvider>()
        ShadowContentResolver.registerProviderInternal(collection.authority, provider)
        whenever(provider.insert(eq(collection), any())).thenReturn(destination)
        resolver.registerOutputStream(destination, object : OutputStream() {
            override fun write(value: Int) = throw IOException(
                "write failed: ENOSPC (No space left on device)",
                ErrnoException("write", OsConstants.ENOSPC),
            )
        })
        val attachment = mock<SceytAttachment> {
            on { filePath } doReturn source.path
            on { name } doReturn source.name
        }
        val item = mock<MediaItem> { on { this.attachment } doReturn attachment }
        val activity = Robolectric.buildActivity(GallerySaveMediaPreviewActivity::class.java).setup().get()

        activity.saveToGallery(item)

        val dialog = ShadowDialog.getLatestDialog() as SceytDialog
        assertThat(dialog.isShowing).isTrue()
        assertThat(dialog.findViewById<TextView>(R.id.textTitle).text.toString())
            .isEqualTo(activity.getString(R.string.sceyt_storage_full_title))
        assertThat(dialog.findViewById<TextView>(R.id.textDescription).text.toString())
            .isEqualTo(activity.getString(R.string.sceyt_media_save_storage_full))
        assertThat(dialog.findViewById<TextView>(R.id.buttonAccept).text.toString())
            .isEqualTo(activity.getString(R.string.sceyt_ok))
        assertThat(dialog.findViewById<View>(R.id.buttonCancel).isVisible).isFalse()
        assertThat(ShadowToast.getLatestToast()).isNull()
        verify(provider).delete(destination, null, null)
    }
}

class GallerySaveMediaPreviewActivity : MediaPreviewActivity() {
    var savedItem: MediaItem? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.SceytPreviewThemeDark)
        super.onCreate(savedInstanceState)
    }

    override fun initPageWithData() = Unit

    override fun initViewModel() = Unit

    override fun save(item: MediaItem) {
        savedItem = item
    }

    fun saveToGallery(item: MediaItem) = super.save(item)

    fun tapSave(item: MediaItem) {
        showActionsDialog(item)
        ShadowDialog.getLatestDialog().findViewById<View>(R.id.save).performClick()
    }
}
