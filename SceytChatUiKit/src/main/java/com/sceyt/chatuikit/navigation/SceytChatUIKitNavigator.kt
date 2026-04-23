package com.sceyt.chatuikit.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

interface SceytChatUIKitNavigator {
    fun resolve(destination: Destination): Destination = destination
}

fun SceytChatUIKitNavigator.navigate(
    context: Context,
    destination: Destination,
) {
    resolve(destination).navigate(context)
}

fun SceytChatUIKitNavigator.navigateForResult(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
    destination: Destination,
) {
    resolve(destination).navigateForResult(context, launcher)
}
