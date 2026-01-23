package com.sceyt.chatuikit.styles.camera

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.StyleCustomizer

data class CameraMediaPreviewStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val bottomControlsBackgroundColor: Int,
        val retakeIcon: Drawable?,
        val confirmIcon: Drawable?,
        val playIcon: Drawable?,
        val pauseIcon: Drawable?,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<CameraMediaPreviewStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): CameraMediaPreviewStyle {
            context.obtainStyledAttributes(attrs, R.styleable.CameraMediaPreview).use { array ->
                val backgroundColor = array.getColor(
                    R.styleable.CameraMediaPreview_sceytUiCameraMediaPreviewBackgroundColor,
                    context.getCompatColor(android.R.color.black)
                )
                val bottomControlsBackgroundColor = array.getColor(
                    R.styleable.CameraMediaPreview_sceytUiCameraMediaPreviewBottomControlsBackgroundColor,
                    0xFF17191C.toInt()
                )

                val retakeIcon = array.getDrawable(R.styleable.CameraMediaPreview_sceytUiCameraMediaPreviewRetakeIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_camera_retake_with_bg)
                val confirmIcon = array.getDrawable(R.styleable.CameraMediaPreview_sceytUiCameraMediaPreviewConfirmIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_camera_confirm_with_bg)
                val playIcon = array.getDrawable(R.styleable.CameraMediaPreview_sceytUiCameraMediaPreviewPlayIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_play_with_bg_32)
                val pauseIcon = array.getDrawable(R.styleable.CameraMediaPreview_sceytUiCameraMediaPreviewPauseIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_pause_with_bg_32)

                return CameraMediaPreviewStyle(
                    backgroundColor = backgroundColor,
                    bottomControlsBackgroundColor = bottomControlsBackgroundColor,
                    retakeIcon = retakeIcon,
                    confirmIcon = confirmIcon,
                    playIcon = playIcon,
                    pauseIcon = pauseIcon,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}