package com.sceyt.chatuikit.styles.extensions.camera

import android.content.res.TypedArray
import androidx.annotation.DrawableRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.applyTintBackgroundLayer
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.camera.CustomCameraStyle
import com.sceyt.chatuikit.styles.common.TextStyle

internal fun CustomCameraStyle.Builder.buildModeTextStyle(
    array: TypedArray
): TextStyle {
    return TextStyle.Builder(array)
        .setSize(R.styleable.CustomCamera_sceytUiCustomCameraModeTextSize, 14f.dpToPx().toInt())
        .setStyle(R.styleable.CustomCamera_sceytUiCustomCameraModeTextStyle)
        .setFont(R.styleable.CustomCamera_sceytUiCustomCameraModeTextFont, R.font.roboto_medium)
        .build()
}

internal fun CustomCameraStyle.Builder.buildRecordingTimeTextStyle(
    array: TypedArray
): TextStyle {
    return TextStyle.Builder(array)
        .setColor(
            R.styleable.CustomCamera_sceytUiCustomCameraRecordingTimeTextColor,
            context.getCompatColor(android.R.color.white)
        )
        .setSize(R.styleable.CustomCamera_sceytUiCustomCameraRecordingTimeTextSize)
        .setStyle(R.styleable.CustomCamera_sceytUiCustomCameraRecordingTimeTextStyle)
        .setFont(R.styleable.CustomCamera_sceytUiCustomCameraRecordingTimeTextFont)
        .build()
}

internal fun CustomCameraStyle.Builder.buildZoomTextStyle(
    array: TypedArray
): TextStyle {
    return TextStyle.Builder(array)
        .setColor(
            R.styleable.CustomCamera_sceytUiCustomCameraZoomTextColor,
            context.getCompatColor(android.R.color.white)
        )
        .setSize(R.styleable.CustomCamera_sceytUiCustomCameraZoomTextSize, 14f.dpToPx().toInt())
        .setStyle(R.styleable.CustomCamera_sceytUiCustomCameraZoomTextStyle)
        .setFont(R.styleable.CustomCamera_sceytUiCustomCameraZoomTextFont)
        .build()
}

internal fun CustomCameraStyle.Builder.buildIconDrawable(
    array: TypedArray,
    iconAttr: Int,
    @DrawableRes defaultRes: Int
) = array.getDrawable(iconAttr)
    ?: context.getCompatDrawable(defaultRes)
        ?.applyTint(array.getColor(R.styleable.CustomCamera_sceytUiCustomCameraIconTintColor, 0))
        ?.applyBackgroundTintIfSet(array.getColor(R.styleable.CustomCamera_sceytUiCustomCameraIconBackgroundTintColor, 0))

private fun android.graphics.drawable.Drawable?.applyBackgroundTintIfSet(color: Int) =
    if (color == 0) this else this.applyTintBackgroundLayer(color, R.id.backgroundLayer)