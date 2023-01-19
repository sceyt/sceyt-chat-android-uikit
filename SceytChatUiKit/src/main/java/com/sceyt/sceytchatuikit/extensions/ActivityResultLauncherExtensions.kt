package com.sceyt.sceytchatuikit.extensions

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

fun ComponentActivity.initPermissionLauncher(callback: ActivityResultCallback<Boolean>): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission(), callback)
}

fun Fragment.initPermissionLauncher(callback: ActivityResultCallback<Boolean>): ActivityResultLauncher<String> {
    return registerForActivityResult(ActivityResultContracts.RequestPermission(), callback)
}

fun ComponentActivity.initCameraLauncher(callback: ActivityResultCallback<Boolean>): ActivityResultLauncher<Uri> {
    return registerForActivityResult(ActivityResultContracts.TakePicture(), callback)
}

fun ComponentActivity.initVideoCameraLauncher(callback: ActivityResultCallback<Boolean>): ActivityResultLauncher<Uri> {
    return registerForActivityResult(ActivityResultContracts.CaptureVideo(), callback)
}

fun Fragment.initCameraLauncher(callback: ActivityResultCallback<Boolean>): ActivityResultLauncher<Uri> {
    return registerForActivityResult(ActivityResultContracts.TakePicture(), callback)
}

fun Fragment.initVideoCameraLauncher(callback: ActivityResultCallback<Boolean>): ActivityResultLauncher<Uri> {
    return registerForActivityResult(ActivityResultContracts.CaptureVideo(), callback)
}

fun ComponentActivity.initAttachmentLauncher(callback: ActivityResultCallback<ActivityResult>): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult(), callback)
}

fun Fragment.initAttachmentLauncher(callback: ActivityResultCallback<ActivityResult>): ActivityResultLauncher<Intent> {
    return registerForActivityResult(ActivityResultContracts.StartActivityForResult(), callback)
}