package com.sceyt.chatuikit.styles.extensions.channel_list

import android.content.res.TypedArray
import android.graphics.Typeface
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.spToPx
import com.sceyt.chatuikit.styles.channel.ChannelItemStyle
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle

internal fun ChannelItemStyle.Builder.buildSubjectTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListChannelSubjectTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListChannelSubjectTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListChannelSubjectTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListChannelSubjectTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun ChannelItemStyle.Builder.buildLastMessageTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListLastMessageTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListLastMessageTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListLastMessageTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListLastMessageTextFont
    )
    .build()

internal fun ChannelItemStyle.Builder.buildDateTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListDateTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListDateTextSize,
        defValue = 13f.spToPx().toInt()
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListDateTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListDateTextFont
    )
    .build()

internal fun ChannelItemStyle.Builder.buildLastMessageSenderNameStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListMessageSenderNameTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListMessageSenderNameTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListMessageSenderNameTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListMessageSenderNameTextFont,
    )
    .build()

internal fun ChannelItemStyle.Builder.buildDeletedTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListDeletedTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListDeletedTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListDeletedTextStyle,
        defValue = Typeface.ITALIC
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListDeletedTextFont
    )
    .build()

internal fun ChannelItemStyle.Builder.buildDraftPrefixTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListDraftPrefixTextColor,
        defValue = context.getCompatColor(R.color.sceyt_color_red)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListDraftPrefixTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListDraftPrefixTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListDraftTextFont
    )
    .build()

internal fun ChannelItemStyle.Builder.buildChannelEventTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListChannelEventTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListChannelEventTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListChannelEventTextStyle,
        defValue = Typeface.ITALIC
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListChannelEventTextFont
    )
    .build()

internal fun ChannelItemStyle.Builder.buildUnreadCountTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountTextFont
    )
    .build()

internal fun ChannelItemStyle.Builder.buildUnreadCountMutedTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedStateBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface3Color)
    )
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedStateTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedStateTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedStateTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadCountMutedTextFont
    )
    .build()


internal fun ChannelItemStyle.Builder.buildMentionTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionTextStyle,
        defValue = Typeface.BOLD
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionTextFont
    )
    .build()


internal fun ChannelItemStyle.Builder.buildUnreadMentionBackgroundStyle(
        array: TypedArray
) = BackgroundStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadMentionBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setShape(Shape.Circle)
    .build()


internal fun ChannelItemStyle.Builder.buildUnreadMentionMutedBackgroundStyle(
        array: TypedArray
) = BackgroundStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.ChannelListView_sceytUiChannelListUnreadMentionMutedStateBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface3Color)
    )
    .setShape(Shape.Circle)
    .build()


internal fun ChannelItemStyle.Builder.buildAvatarTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListAvatarTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListAvatarTextSize
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListAvatarTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListAvatarTextFont
    )
    .build()

@Suppress("UnusedReceiverParameter")
internal fun ChannelItemStyle.Builder.buildAvatarShape(
        array: TypedArray
): Shape {
    val value = array.getInt(R.styleable.ChannelListView_sceytUiChannelListAvatarShape, 0)
    val cornerRadius = array.getDimension(R.styleable.ChannelListView_sceytUiChannelListAvatarCornerRadius, 0f)
    return if (value == 1) {
        Shape.RoundedCornerShape(cornerRadius)
    } else Shape.Circle
}

internal fun ChannelItemStyle.Builder.buildAvatarStyle(
        array: TypedArray
) = AvatarStyle.Builder(array)
    .avatarBackgroundColor(
        index = R.styleable.ChannelListView_sceytUiChannelListAvatarBackgroundColor
    )
    .textStyle(buildAvatarTextStyle(array))
    .shape(buildAvatarShape(array))
    .build()
