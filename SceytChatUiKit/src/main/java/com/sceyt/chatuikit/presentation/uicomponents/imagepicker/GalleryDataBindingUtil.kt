package com.sceyt.chatuikit.presentation.uicomponents.imagepicker

import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytstyles.GalleryPickerStyle

object GalleryDataBindingUtil {

    @BindingAdapter("setGalleryItemCheckedState")
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