package com.sceyt.chatuikit.styles.extensions.messages_list

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import com.sceyt.chatuikit.styles.messages_list.item.SelfDestructedMessageItemStyle

internal fun MessageItemStyle.Builder.buildSelfDestructedMessageItemStyle(
    typedArray: TypedArray,
) = SelfDestructedMessageItemStyle.Builder(context, typedArray).build()

internal fun SelfDestructedMessageItemStyle.Builder.buildSelfDestructedMessageIconColor(
) = typedArray.getColor(
    R.styleable.MessagesListView_sceytUiMessagesListSelfDestructedMessageIconTint,
    context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
)
