package com.sceyt.chatuikit.presentation.components.camera

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.navigateForResult
import com.sceyt.chatuikit.presentation.components.picker.BottomSheetMediaPicker

class CameraNavigator(
    private val activity: Activity,
    private val previewLauncher: ActivityResultLauncher<Intent>
) {
    fun openPhotoPreview(filePath: String) {
        SceytChatUIKit.navigator.navigateForResult(
            context = activity,
            launcher = previewLauncher,
            destination = Destination.CameraMediaPreview(filePath, false)
        )
    }

    fun openVideoPreview(filePath: String) {
        SceytChatUIKit.navigator.navigateForResult(
            context = activity,
            launcher = previewLauncher,
            destination = Destination.CameraMediaPreview(filePath, true)
        )
    }

    fun returnResult(filePath: String, isVideo: Boolean) {
        val resultIntent = Intent().apply {
            putExtra(CustomCameraActivity.EXTRA_RESULT_URI, filePath)
            putExtra(CustomCameraActivity.EXTRA_IS_VIDEO, isVideo)
        }
        activity.setResult(Activity.RESULT_OK, resultIntent)
        activity.finish()
    }

    fun returnSelectedMedia(selectedMedia: List<BottomSheetMediaPicker.SelectedMediaData>) {
        if (selectedMedia.isEmpty()) return

        val resultIntent = Intent().apply {
            putParcelableArrayListExtra(
                CustomCameraActivity.EXTRA_RESULT_SELECTED_MEDIA,
                ArrayList(selectedMedia)
            )
        }
        activity.setResult(Activity.RESULT_OK, resultIntent)
        activity.finish()
    }
}