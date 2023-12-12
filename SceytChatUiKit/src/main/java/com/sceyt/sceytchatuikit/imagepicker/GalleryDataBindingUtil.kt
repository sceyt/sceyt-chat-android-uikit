package com.sceyt.sceytchatuikit.imagepicker

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.sceyt.sceytchatuikit.sceytstyles.GalleryPickerStyle

object GalleryDataBindingUtil {

    @BindingAdapter("setGalleryItemCheckedState")
    @JvmStatic
    fun setGalleryItemCheckedState(image: ImageView, isChecked: Boolean) {
        if (isChecked) {
            image.setImageResource(GalleryPickerStyle.checkedStateIcon)
        } else {
            image.setImageResource(GalleryPickerStyle.unCheckedStateIcon)
        }
    }
}