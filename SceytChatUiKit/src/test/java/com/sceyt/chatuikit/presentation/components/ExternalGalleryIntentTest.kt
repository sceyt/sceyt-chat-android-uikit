package com.sceyt.chatuikit.presentation.components

import android.content.Intent
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExternalGalleryIntentTest {
    @Test
    fun `profile photo requests single image selection`() {
        val intent = galleryIntent(allowMultiple = false, onlyImages = true)

        assertThat(intent.action).isEqualTo(MediaStore.ACTION_PICK_IMAGES)
        assertThat(intent.type).isEqualTo("image/*")
        assertThat(intent.hasExtra(MediaStore.EXTRA_PICK_IMAGES_MAX)).isFalse()
    }

    @Test
    fun `multiple media uses the photo picker's multiple selection limit`() {
        val intent = galleryIntent(allowMultiple = true, onlyImages = false)

        assertThat(intent.action).isEqualTo(MediaStore.ACTION_PICK_IMAGES)
        assertThat(intent.type).isNull()
        assertThat(intent.getIntExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 0)).isGreaterThan(1)
    }

    @Test
    @Config(sdk = [28])
    fun `older devices can select photos and videos via the document picker`() {
        val intent = galleryIntent(allowMultiple = true, onlyImages = false)

        assertThat(intent.action).isEqualTo(Intent.ACTION_OPEN_DOCUMENT)
        assertThat(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)).isTrue()
        assertThat(intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.asList())
            .containsExactly("image/*", "video/*")
    }

    private fun galleryIntent(allowMultiple: Boolean, onlyImages: Boolean): Intent {
        return Robolectric.buildActivity(GalleryActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.picker.chooseFromGallery(allowMultiple = allowMultiple, onlyImages = onlyImages) {}
            requireNotNull(shadowOf(activity).nextStartedActivityForResult).intent
        }
    }

    class GalleryActivity : ComponentActivity() {
        val picker = FilePickerHelper(this)
    }
}
