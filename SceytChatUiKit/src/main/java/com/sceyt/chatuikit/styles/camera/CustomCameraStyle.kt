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
import com.sceyt.chatuikit.styles.extensions.camera.buildIconDrawable
import com.sceyt.chatuikit.styles.extensions.camera.buildModeTextStyle
import com.sceyt.chatuikit.styles.extensions.camera.buildRecordingTimeTextStyle
import com.sceyt.chatuikit.styles.extensions.camera.buildZoomTextStyle

data class CustomCameraStyle(
        @param:ColorInt val backgroundColor: Int,
        val iconBackground: Drawable?,
        val modeSelectorStyle: ModeSelectorStyle,
        val captureStyle: CaptureStyle,
        val recordingTimeStyle: RecordingTimeStyle,
        val zoomStyle: ZoomStyle,
        val focusIndicatorStyle: FocusIndicatorStyle,
        val closeIcon: Drawable?,
        val switchCameraIcon: Drawable?,
        val galleryIcon: Drawable?,
        val flashStyle: FlashStyle,
        val pauseIcon: Drawable?,
        val playIcon: Drawable?,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<CustomCameraStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): CustomCameraStyle {
            context.obtainStyledAttributes(attrs, R.styleable.CustomCamera).use { array ->
                val backgroundColor = array.getColor(
                    R.styleable.CustomCamera_sceytUiCustomCameraBackgroundColor,
                    context.getCompatColor(android.R.color.black)
                )
                val modeSelectorBackgroundColor = array.getColor(
                    R.styleable.CustomCamera_sceytUiCustomCameraModeSelectorBackgroundColor,
                    0xFF17191C.toInt()
                )

                val iconBackground = if (array.hasValue(R.styleable.CustomCamera_sceytUiCustomCameraIconBackground)) {
                    array.getDrawable(R.styleable.CustomCamera_sceytUiCustomCameraIconBackground)
                } else {
                    null
                }
                val captureButtonBackground = if (array.hasValue(R.styleable.CustomCamera_sceytUiCustomCameraCaptureBackground)) {
                    array.getDrawable(R.styleable.CustomCamera_sceytUiCustomCameraCaptureBackground)
                } else {
                    null
                }
                val recordingTimeBackground = array.getDrawable(R.styleable.CustomCamera_sceytUiCustomCameraRecordingTimeBackground)
                    ?: context.getCompatDrawable(R.drawable.sceyt_bg_mode_highlight)
                val zoomBackground = array.getDrawable(R.styleable.CustomCamera_sceytUiCustomCameraZoomBackground)
                    ?: context.getCompatDrawable(R.drawable.sceyt_bg_mode_highlight)
                val focusIcon = array.getDrawable(R.styleable.CustomCamera_sceytUiCustomCameraFocusIndicatorBackground)
                    ?: context.getCompatDrawable(R.drawable.sceyt_bg_focus_circle)
                val modeHighlightBackground = array.getDrawable(R.styleable.CustomCamera_sceytUiCustomCameraModeHighlightBackground)
                    ?: context.getCompatDrawable(R.drawable.sceyt_bg_mode_highlight)

                val closeIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraCloseIcon,
                    R.drawable.sceyt_ic_camera_close_with_bg
                )
                val switchCameraIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraSwitchCameraIcon,
                    R.drawable.sceyt_ic_camera_rotate_with_bg
                )
                val galleryIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraGalleryIcon,
                    R.drawable.sceyt_ic_camera_gallery_with_bg
                )
                val flashOffIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraFlashOffIcon,
                    R.drawable.sceyt_ic_flash_off_with_bg
                )
                val flashOnIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraFlashOnIcon,
                    R.drawable.sceyt_ic_flash_on_with_bg
                )
                val flashAutoIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraFlashAutoIcon,
                    R.drawable.sceyt_ic_flash_auto_with_bg
                )
                val capturePhotoIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraCapturePhotoIcon,
                    R.drawable.sceyt_ic_capture_selector
                )
                val captureVideoIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraCaptureVideoIcon,
                    R.drawable.sceyt_ic_record_selector
                )
                val stopRecordingIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraStopRecordingIcon,
                    R.drawable.sceyt_ic_stop
                )
                val pauseIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraPauseIcon,
                    R.drawable.sceyt_ic_pause_with_bg_28
                )
                val playIcon = buildIconDrawable(
                    array,
                    R.styleable.CustomCamera_sceytUiCustomCameraPlayIcon,
                    R.drawable.sceyt_ic_play_with_bg_28
                )

                val photoModeText = array.getString(R.styleable.CustomCamera_sceytUiCustomCameraModePhotoText)
                    ?: context.getString(R.string.sceyt_camera_mode_photo)
                val videoModeText = array.getString(R.styleable.CustomCamera_sceytUiCustomCameraModeVideoText)
                    ?: context.getString(R.string.sceyt_camera_mode_video)

                val selectedTextColor = array.getColor(
                    R.styleable.CustomCamera_sceytUiCustomCameraModeTextColor,
                    context.getCompatColor(android.R.color.white)
                )
                val unselectedTextColor = array.getColor(
                    R.styleable.CustomCamera_sceytUiCustomCameraModeTextUnselectedColor,
                    context.getCompatColor(android.R.color.white)
                )

                val modeTextStyle = buildModeTextStyle(array)
                val recordingTimeTextStyle = buildRecordingTimeTextStyle(array)
                val zoomTextStyle = buildZoomTextStyle(array)
                val focusIndicatorAnimDurationMs = array.getInt(
                    R.styleable.CustomCamera_sceytUiCustomCameraFocusIndicatorAnimDuration,
                    1000
                ).toLong()

                return CustomCameraStyle(
                    backgroundColor = backgroundColor,
                    iconBackground = iconBackground,
                    modeSelectorStyle = ModeSelectorStyle(
                        backgroundColor = modeSelectorBackgroundColor,
                        selectedTextColor = selectedTextColor,
                        unselectedTextColor = unselectedTextColor,
                        highlightBackground = modeHighlightBackground,
                        photoText = photoModeText,
                        videoText = videoModeText,
                        textStyle = modeTextStyle,
                    ),
                    captureStyle = CaptureStyle(
                        buttonBackground = captureButtonBackground,
                        photoIcon = capturePhotoIcon,
                        videoIcon = captureVideoIcon,
                        stopRecordingIcon = stopRecordingIcon,
                    ),
                    recordingTimeStyle = RecordingTimeStyle(
                        background = recordingTimeBackground,
                        textStyle = recordingTimeTextStyle,
                    ),
                    zoomStyle = ZoomStyle(
                        background = zoomBackground,
                        textStyle = zoomTextStyle,
                    ),
                    focusIndicatorStyle = FocusIndicatorStyle(
                        focusIcon = focusIcon,
                        animDurationMs = focusIndicatorAnimDurationMs,
                    ),
                    closeIcon = closeIcon,
                    switchCameraIcon = switchCameraIcon,
                    galleryIcon = galleryIcon,
                    flashStyle = FlashStyle(
                        offIcon = flashOffIcon,
                        onIcon = flashOnIcon,
                        autoIcon = flashAutoIcon,
                    ),
                    pauseIcon = pauseIcon,
                    playIcon = playIcon,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}