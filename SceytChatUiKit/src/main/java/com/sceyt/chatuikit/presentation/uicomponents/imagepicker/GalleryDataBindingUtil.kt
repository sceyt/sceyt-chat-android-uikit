package com.sceyt.chatuikit.presentation.uicomponents.imagepicker

import android.graphics.Color
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.sceytstyles.GalleryPickerStyle

object GalleryDataBindingUtil {

    @BindingAdapter("setGalleryItemCheckedState")
    @JvmStatic
    fun setGalleryItemCheckedState(image: ImageView, isChecked: Boolean) {
        with(image) {
            if (isChecked) {
                setBackgroundTintColorRes(SceytChatUIKit.theme.accentColor)
                setImageResource(GalleryPickerStyle.checkedStateIcon)
            } else {
                setBackgroundTint(Color.TRANSPARENT)
                setImageResource(GalleryPickerStyle.unCheckedStateIcon)
            }
        }
    }
}