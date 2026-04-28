package com.sceyt.chatuikit.navigation

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher

/**
 * Resolves UIKit navigation targets before they are launched.
 *
 * Host apps can override [resolve] to replace any UIKit [Destination] with their own
 * destination subclass or replacement. UIKit still decides whether the flow is regular
 * navigation or result-based navigation, so the app only has to replace a destination once.
 */
interface SceytChatUIKitNavigator {
    /**
     * Returns the destination that should be launched for the requested UIKit route.
     *
     * Return [destination] to keep the default UIKit behavior, or return a custom
     * destination to change the target activity, intent extras, flags, or transition.
     */
    fun resolve(destination: Destination): Destination = destination
}

/**
 * Launches a UIKit destination after giving the app navigator a chance to replace it.
 *
 * This helper is used for one-way navigation. Override [SceytChatUIKitNavigator.resolve]
 * rather than this function to customize a screen.
 */
fun SceytChatUIKitNavigator.navigate(
    context: Context,
    destination: Destination,
) {
    resolve(destination).navigate(context)
}

/**
 * Launches a UIKit destination for result after giving the app navigator a chance to replace it.
 *
 * Result parsing stays in the caller that owns [launcher]. The navigator only resolves
 * which destination should be opened.
 */
fun SceytChatUIKitNavigator.navigateForResult(
    context: Context,
    launcher: ActivityResultLauncher<Intent>,
    destination: Destination,
) {
    resolve(destination).navigateForResult(context, launcher)
}
