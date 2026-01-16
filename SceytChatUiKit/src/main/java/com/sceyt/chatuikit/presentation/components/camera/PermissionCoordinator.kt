package com.sceyt.chatuikit.presentation.components.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.permissionIgnored
import com.sceyt.chatuikit.presentation.common.dialogs.SceytDialog

class PermissionCoordinator(
    private val activity: Activity,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>
) {
    val basePermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun requestBasePermissions() {
        permissionLauncher.launch(basePermissions)
    }

    fun requestAudioPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }

    fun handleCameraDenied() {
        if (activity.permissionIgnored(Manifest.permission.CAMERA)) {
            SceytDialog.showDialog(
                context = activity,
                titleId = R.string.sceyt_camera_permission_disabled_title,
                descId = R.string.sceyt_camera_permission_disabled_desc,
                positiveBtnTitleId = R.string.sceyt_settings,
                negativeBtnTitleId = R.string.sceyt_cancel,
                positiveCb = { activity.finish() },
                negativeCb = { activity.finish() }
            )
        } else {
            activity.finish()
        }
    }
}
