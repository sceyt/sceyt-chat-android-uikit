package com.sceyt.chat.ui.sceytconfigs

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorRes
import com.sceyt.chat.ui.R

object ChannelStyle {
    @ColorRes
    var titleColor: Int = R.color.colorFontDark

    @ColorRes
    var lastMessageTextColor: Int = R.color.colorFontGray

    @ColorRes
    var unreadCountColor: Int = R.color.colorAccent

    /*internal constructor(context: Context) : this(
        titleColor = context.getCompatColor(R.color.colorFontDark),
        lastMessageTextColor = context.getCompatColor(R.color.colorFontGray),
        unreadCountColor = context.getCompatColor(R.color.colorAccent)
    )*/

    /* internal constructor(context: Context, typedArray: TypedArray) : this(
         titleColor = typedArray.getResourceId(
             R.styleable.ChannelListView_sceytUiChannelTitleTextColor,
             R.color.colorFontDark
         ),
         lastMessageTextColor = typedArray.getColor(
             R.styleable.ChannelListView_sceytUiLastMessageTextColor,
             context.getCompatColor(R.color.colorFontGray)
         ),
         unreadCountColor = typedArray.getColor(
             R.styleable.ChannelListView_sceytUiUnreadMessageCounterTextColor,
             context.getCompatColor(R.color.colorAccent)
         )
     )*/


    internal fun updateWithAttributes(typedArray: TypedArray): ChannelStyle {
        titleColor = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiChannelTitleTextColor, titleColor)
        lastMessageTextColor = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiLastMessageTextColor, lastMessageTextColor)
        unreadCountColor = typedArray.getResourceId(R.styleable.ChannelListView_sceytUiUnreadMessageCounterTextColor, unreadCountColor)
        return this
    }
}