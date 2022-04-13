package com.sceyt.chat.ui.sceytconfigs

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.extencions.getCompatColor

data class ChannelStyle(
        @ColorInt
        var titleColor: Int,
        @ColorInt
        var lastMessageTextColor: Int,
        @ColorInt
        var unreadCountColor: Int,
) {

    internal constructor(context: Context) : this(
        titleColor = context.getCompatColor(R.color.colorFontDark),
        lastMessageTextColor = context.getCompatColor(R.color.colorFontGray),
        unreadCountColor = context.getCompatColor(R.color.colorAccent)
    )

     internal constructor(context: Context, typedArray: TypedArray) : this(
         titleColor = typedArray.getColor(
             R.styleable.ChannelListView_sceytUiChannelTitleTextColor,
             context.getCompatColor(R.color.colorFontDark)
         ),
         lastMessageTextColor = typedArray.getColor(
             R.styleable.ChannelListView_sceytUiLastMessageTextColor,
             context.getCompatColor(R.color.colorFontGray)
         ),
         unreadCountColor = typedArray.getColor(
             R.styleable.ChannelListView_sceytUiUnreadMessageCounterTextColor,
             context.getCompatColor(R.color.colorAccent)
         )
     )


    internal fun updateWithAttributes(typedArray: TypedArray): ChannelStyle {
        titleColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiChannelTitleTextColor, titleColor)
        lastMessageTextColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiLastMessageTextColor, lastMessageTextColor)
        unreadCountColor = typedArray.getColor(R.styleable.ChannelListView_sceytUiUnreadMessageCounterTextColor, unreadCountColor)
        return this
    }
}