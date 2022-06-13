package com.sceyt.chat.ui.sceytconfigs

import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.chat.ui.R

object ConversationHeaderViewStyle {
    @DrawableRes
    var backIcon: Int = R.drawable.sceyt_ic_arrow_back

    @ColorRes
    var titleColor: Int = R.color.sceyt_color_text_themed

    @ColorRes
    var subTitleColor: Int = R.color.sceyt_color_gray_400


    internal fun updateWithAttributes(typedArray: TypedArray): ConversationHeaderViewStyle {
        backIcon = typedArray.getResourceId(R.styleable.ConversationHeaderView_sceytConvHeaderBackIcon, backIcon)
        titleColor = typedArray.getResourceId(R.styleable.ConversationHeaderView_sceytConvHeaderTitleColor, titleColor)
        subTitleColor = typedArray.getResourceId(R.styleable.ConversationHeaderView_sceytConvHeaderSubTitleColor, subTitleColor)
       return this
    }
}