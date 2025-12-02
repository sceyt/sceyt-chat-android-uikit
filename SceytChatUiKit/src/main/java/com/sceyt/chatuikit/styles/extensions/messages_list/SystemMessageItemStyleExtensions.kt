package com.sceyt.chatuikit.styles.extensions.messages_list

import android.content.res.TypedArray
import android.graphics.Color
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle
import com.sceyt.chatuikit.styles.messages_list.item.SystemMessageItemStyle

internal fun SystemMessageItemStyle.Builder.buildTextStyle() = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListView_sceytUiMessagesListSystemMessageTextColor,
        defValue = Color.WHITE
    )
    .setSize(
        index = R.styleable.MessagesListView_sceytUiMessagesListSystemMessageTextSize,
        defValue = context.resources.getDimensionPixelSize(R.dimen.extraSmallTextSize)
    )
    .setStyle(
        index = R.styleable.MessagesListView_sceytUiMessagesListSystemMessageTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListView_sceytUiMessagesListSystemMessageTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun SystemMessageItemStyle.Builder.buildBackgroundStyle(): BackgroundStyle {
    val cornerRadius = typedArray.getDimension(
        R.styleable.MessagesListView_sceytUiMessagesListSystemMessageCornersRadius,
        dpToPx(30f).toFloat()
    )
    return BackgroundStyle.Builder(typedArray)
        .setBackgroundColor(
            index = R.styleable.MessagesListView_sceytUiMessagesListSystemMessageBackgroundColor,
            defValue = context.getCompatColor(SceytChatUIKit.theme.colors.overlayBackgroundColor)
        )
        .setBorderColor(
            index = R.styleable.MessagesListView_sceytUiMessagesListSystemMessageBorderColor
        )
        .setBorderWidth(
            index = R.styleable.MessagesListView_sceytUiMessagesListSystemMessageBorderWidth
        )
        .setShape(
            Shape.RoundedCornerShape(radius = cornerRadius)
        )
        .build()
}

internal fun MessageItemStyle.Builder.buildSystemMessageItemStyle(
    typedArray: TypedArray,
) = SystemMessageItemStyle.Builder(context, typedArray).build()