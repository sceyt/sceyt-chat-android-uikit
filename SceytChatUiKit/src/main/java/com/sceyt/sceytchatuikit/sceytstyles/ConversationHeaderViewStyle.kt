package com.sceyt.sceytchatuikit.sceytstyles

import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.sceyt.sceytchatuikit.R

object ConversationHeaderViewStyle {
    @DrawableRes
    var backIcon: Int = R.drawable.sceyt_ic_arrow_back

    @ColorRes
    var titleColor: Int = R.color.sceyt_color_text_themed

    @ColorRes
    var subTitleColor: Int = R.color.sceyt_color_gray_400

    @ColorRes
    var underlineColor: Int = R.color.sceyt_color_divider

    var enableUnderline: Boolean = true


    internal fun updateWithAttributes(typedArray: TypedArray): ConversationHeaderViewStyle {
        backIcon = typedArray.getResourceId(R.styleable.ConversationHeaderView_sceytConvHeaderBackIcon, backIcon)
        titleColor = typedArray.getResourceId(R.styleable.ConversationHeaderView_sceytConvHeaderTitleColor, titleColor)
        subTitleColor = typedArray.getResourceId(R.styleable.ConversationHeaderView_sceytConvHeaderSubTitleColor, subTitleColor)
        underlineColor = typedArray.getResourceId(R.styleable.ConversationHeaderView_sceytConvHeaderUnderlineColor, underlineColor)
        enableUnderline = typedArray.getBoolean(R.styleable.ConversationHeaderView_sceytConvHeaderEnableUnderline, enableUnderline)
        return this
    }
}