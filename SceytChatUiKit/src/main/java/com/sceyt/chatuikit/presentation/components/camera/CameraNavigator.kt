package com.sceyt.chatuikit.presentation.components.camera

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

class CameraNavigator(
    private val activity: Activity,
    private val previewLauncher: ActivityResultLauncher<Intent>
) {
    fun openPhotoPreview(filePath: String) {
        val intent = Intent(activity, CameraMediaPreviewActivity::class.java).apply {
            putExtra(CameraMediaPreviewActivity.EXTRA_FILE_PATH, filePath)
            putExtra(CameraMediaPreviewActivity.EXTRA_IS_VIDEO, false)
        }
        previewLauncher.launch(intent)
    }

    fun openVideoPreview(filePath: String) {
        val intent = Intent(activity, CameraMediaPreviewActivity::class.java).apply {
            putExtra(CameraMediaPreviewActivity.EXTRA_FILE_PATH, filePath)
            putExtra(CameraMediaPreviewActivity.EXTRA_IS_VIDEO, true)
        }
        previewLauncher.launch(intent)
    }

    fun returnResult(filePath: String, isVideo: Boolean) {
        val resultIntent = Intent().apply {
            putExtra(CustomCameraActivity.EXTRA_RESULT_URI, filePath)
            putExtra(CustomCameraActivity.EXTRA_IS_VIDEO, isVideo)
        }
        activity.setResult(Activity.RESULT_OK, resultIntent)
        activity.finish()
    }
}