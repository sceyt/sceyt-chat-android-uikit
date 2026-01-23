package com.sceyt.chatuikit.styles.extensions.messages_list

import android.content.res.TypedArray
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import com.sceyt.chatuikit.styles.messages_list.item.SelfDestructedMessageItemStyle

internal fun MessageItemStyle.Builder.buildSelfDestructedMessageItemStyle(
    typedArray: TypedArray,
) = SelfDestructedMessageItemStyle.Builder(context, typedArray).build()