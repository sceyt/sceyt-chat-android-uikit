package com.sceyt.chatuikit.styles.cropper

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.withAlpha
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.yalantis.ucrop.UCrop

data class ImageCropperStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val toolbarColor: Int,
        @param:ColorInt val toolbarIconsColor: Int,
        @param:ColorInt val statusBarColor: Int,
        @param:ColorInt val maskColor: Int,
        @param:DrawableRes val cancelIcon: Int,
        @param:DrawableRes val confirmIcon: Int,
        val toolbarTitle: String,
) {
    companion object {

        var styleCustomizer = StyleCustomizer<ImageCropperStyle> { _, style -> style }

        fun default(context: Context) = ImageCropperStyle(
            backgroundColor = Color.BLACK,
            maskColor = Color.BLACK.withAlpha(0.4f),
            cancelIcon = R.drawable.sceyt_ic_arrow_back,
            confirmIcon = R.drawable.sceyt_ic_save,
            toolbarTitle = context.getString(R.string.sceyt_move_and_scale),
            statusBarColor = Color.BLACK,
            toolbarColor = Color.BLACK,
            toolbarIconsColor = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor),
        ).let { styleCustomizer.apply(context, it) }
    }

    fun createOptions() = UCrop.Options().apply {
        setToolbarColor(toolbarColor)
        setStatusBarColor(statusBarColor)
        setRootViewBackgroundColor(backgroundColor)
        setDimmedLayerColor(maskColor)
        setToolbarCropDrawable(confirmIcon)
        setToolbarCancelDrawable(cancelIcon)
        setToolbarWidgetColor(toolbarIconsColor)
        setToolbarTitle(toolbarTitle)
        setHideBottomControls(true)
        setCircleDimmedLayer(true)
        setShowCropGrid(false)
        setShowCropFrame(false)
    }
}