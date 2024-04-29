package com.sceyt.chatuikit.sceytstyles

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.presentation.uicomponents.imagepicker.GalleryMediaPicker.Companion.MAX_SELECT_MEDIA_COUNT

data class GalleryPickerStyle(
        @ColorInt var nextButtonColor: Int,
        @ColorInt var counterColor: Int,
        var checkedStateIcon: Drawable?,
        var unCheckedStateIcon: Drawable?,
        var maxSelectCount: Int,
        var videoDurationIcon: Drawable?,
) {

    companion object {
        @JvmField
        var galleryPickerStyleCustomizer = StyleCustomizer<GalleryPickerStyle> { it }
    }

    internal class Builder(
            private val context: Context,
            private val attributeSet: AttributeSet?
    ) {
        fun build(): GalleryPickerStyle {
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.SceytGalleryPicker)

            val nextButtonColor = typedArray.getColor(R.styleable.SceytGalleryPicker_sceytUiPickerNextButtonColor,
                context.getCompatColor(SceytChatUIKit.theme.accentColor))

            val counterColor = typedArray.getColor(R.styleable.SceytGalleryPicker_sceytUiPickerCounterColor,
                context.getCompatColor(SceytChatUIKit.theme.accentColor))

            val checkedStateIcon = typedArray.getDrawable(R.styleable.SceytGalleryPicker_sceytUiPickerCheckedStateIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_gallery_checked_state)

            val unCheckedStateIcon = typedArray.getDrawable(R.styleable.SceytGalleryPicker_sceytUiPickerUnCheckedStateIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_gallery_unchecked_state)

            val maxSelectCount = typedArray.getInt(R.styleable.SceytGalleryPicker_sceytUiPickerMaxSelectCount, MAX_SELECT_MEDIA_COUNT)

            val videoDurationIcon = typedArray.getDrawable(R.styleable.SceytGalleryPicker_sceytUiPickerUnVideoDurationIcon)
                    ?: context.getCompatDrawable(R.drawable.sceyt_ic_video)

            typedArray.recycle()

            return GalleryPickerStyle(
                nextButtonColor = nextButtonColor,
                counterColor = counterColor,
                checkedStateIcon = checkedStateIcon,
                unCheckedStateIcon = unCheckedStateIcon,
                maxSelectCount = maxSelectCount,
                videoDurationIcon = videoDurationIcon
            ).let(galleryPickerStyleCustomizer::apply)
        }
    }
}