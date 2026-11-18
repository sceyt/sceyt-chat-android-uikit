package com.sceyt.chatuikit.presentation.components

import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.common.truth.Truth.assertThat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker
import com.sceyt.chatuikit.shared.helpers.picker.FilePickerHelper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BottomSheetGalleryPermissionTest {
    @Test
    fun `gallery opens directly when only selected media access is granted`() {
        Robolectric.buildActivity(GalleryActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            shadowOf(activity).grantPermissions(READ_MEDIA_VISUAL_USER_SELECTED)

            activity.picker.openMediaPicker()

            assertGalleryOpened(activity)
        }
    }

    @Test
    fun `choosing selected photos in the system permission dialog opens the gallery`() {
        Robolectric.buildActivity(GalleryActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val shadow = shadowOf(activity)
            activity.picker.openMediaPicker()

            val request = requireNotNull(shadow.lastRequestedPermission)
            assertThat(shadow.nextStartedActivityForResult).isNotNull()
            val permissions = request.requestedPermissions
            assertThat(permissions.asList()).containsExactly(
                READ_MEDIA_IMAGES, READ_MEDIA_VIDEO, READ_MEDIA_VISUAL_USER_SELECTED
            )

            shadow.grantPermissions(READ_MEDIA_VISUAL_USER_SELECTED)
            val results = permissions.map {
                if (it == READ_MEDIA_VISUAL_USER_SELECTED) PERMISSION_GRANTED else PERMISSION_DENIED
            }.toIntArray()
            activity.onRequestPermissionsResult(request.requestCode, permissions, results)

            assertGalleryOpened(activity)
        }
    }

    private fun assertGalleryOpened(activity: GalleryActivity) {
        activity.supportFragmentManager.executePendingTransactions()
        val pickers = activity.supportFragmentManager.fragments.filterIsInstance<BottomSheetMediaPicker>()
        assertThat(pickers).hasSize(1)
        assertThat(shadowOf(activity).nextStartedActivityForResult).isNull()
    }

    class GalleryActivity : AppCompatActivity() {
        val picker = FilePickerHelper(this)

        override fun onCreate(savedInstanceState: Bundle?) {
            setTheme(R.style.SceytAppTheme)
            super.onCreate(savedInstanceState)
        }
    }
}
