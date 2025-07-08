package com.sceyt.chatuikit.extensions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

fun Activity.requestPermissionsSafety(vararg permissions: String, requestCode: Int) {
    if (permissions.isEmpty()) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }
}

fun Context.hasPermissions(vararg permission: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    for (per in permission) {
        if (checkSelfPermission(per) != PackageManager.PERMISSION_GRANTED)
            return false
    }
    return true
}

fun Fragment.hasPermissions(vararg permission: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    for (per in permission) {
        if (requireActivity().checkSelfPermission(per) != PackageManager.PERMISSION_GRANTED)
            return false
    }
    return true
}

fun Context.permissionIgnored(permission: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
    if (hasPermissions(permission)) return false
    return !asActivity().shouldShowRequestPermissionRationale(permission)
}

fun Context.oneOfPermissionsIgnored(vararg permission: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
    if (hasPermissions(*permission)) return false

    permission.forEach {
        if (!asActivity().shouldShowRequestPermissionRationale(it)) return true
    }
    return false
}

fun Context.hasOneOfPermissions(vararg permission: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
    for (per in permission) {
        if (checkSelfPermission(per) == PackageManager.PERMISSION_GRANTED)
            return true
    }
    return false
}

fun Fragment.requestPermissionsSafety(permissions: Array<String>, requestCode: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ActivityCompat.requestPermissions(requireActivity(), permissions, requestCode)
    }
}

fun Activity.checkHasOneOfAndPermissionsOrAsk(activityResultLauncher: ActivityResultLauncher<String>, vararg permissions: String): Boolean {
    return if (hasOneOfPermissions(*permissions)) {
        true
    } else {
        for (perm in permissions) {
            if (!hasPermissions(perm)) {
                activityResultLauncher.launch(perm)
                break
            }
        }
        false
    }
}

fun Context.checkAndAskPermissions(
        activityResultLauncher: ActivityResultLauncher<String>?,
        vararg permissions: String
): Boolean {
    return if (hasPermissions(*permissions)) {
        true
    } else {
        for (perm in permissions) {
            if (!hasPermissions(perm)) {
                activityResultLauncher?.launch(perm) ?: run {
                    (this as? Activity)?.requestPermissionsSafety(*permissions, requestCode = 0)
                }
                break
            }
        }
        false
    }
}

fun Context.checkDeniedOneOfPermissions(vararg permissions: String): Boolean {
    if (this !is AppCompatActivity) return false
    for (permission in permissions) {
        if (!hasPermissions(permission) && !shouldShowRequestPermissionRationale(permission))
            return true
    }
    return false
}

fun Context.hasLocationPermission(): Boolean {
    val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    return hasOneOfPermissions(*permissions)
}

fun getPermissionsForMangeStorage(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
    } else
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}