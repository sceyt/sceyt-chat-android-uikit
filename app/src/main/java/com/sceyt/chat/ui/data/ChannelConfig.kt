package com.sceyt.chat.ui.data

import android.content.res.ColorStateList
import android.content.res.TypedArray
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.core.content.ContextCompat
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ItemChannelBinding

object ChannelConfig {

    @ColorRes
    var titleColor = R.color.colorFontDark

    @ColorRes
    var descColor = R.color.colorFontGray

    @ColorRes
    var unreadCountColor = R.color.colorAccent

    @DimenRes
    val titleSize: Int = R.dimen.mediumTextSize


    fun updateWithAttributes(typedArray: TypedArray) {
        titleColor = typedArray.getResourceId(R.styleable.ChannelListView_titleColor, titleColor)
        descColor = typedArray.getResourceId(R.styleable.ChannelListView_descColor, descColor)
        unreadCountColor = typedArray.getResourceId(R.styleable.ChannelListView_unreadCountColor, unreadCountColor)
    }

    fun ItemChannelBinding.setChannelItemStyle() {
        channelTitle.setTextColor(ContextCompat.getColor(root.context, titleColor))
        lastMessage.setTextColor(ContextCompat.getColor(root.context, descColor))
        messageCount.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(root.context, unreadCountColor))
    }
}