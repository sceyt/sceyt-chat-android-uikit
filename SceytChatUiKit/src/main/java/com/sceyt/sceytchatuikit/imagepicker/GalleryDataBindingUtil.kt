package com.sceyt.sceytchatuikit.imagepicker

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.GalleryPickerStyle

object GalleryDataBindingUtil {

    @BindingAdapter("bind:setGalleryItemCheckedState")
    @JvmStatic
    fun setGalleryItemCheckedState(image: ImageView, isChecked: Boolean) {
        with(image) {
            if (isChecked) {
                image.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
                image.setImageResource(GalleryPickerStyle.checkedStateIcon)
            } else {
                image.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
                image.setImageResource(GalleryPickerStyle.unCheckedStateIcon)
            }
        }
    }
}