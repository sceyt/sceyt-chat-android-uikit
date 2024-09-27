package com.sceyt.chatuikit.styles.extensions

import android.content.res.TypedArray
import android.graphics.Typeface
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.ChannelItemStyle
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

internal fun ChannelItemStyle.Builder.buildTypingTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListTypingTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListTypingTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListTypingTextStyle,
        defValue = Typeface.ITALIC
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListTypingTextFont
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
    .setBackgroundColor(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
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


internal fun ChannelItemStyle.Builder.buildMentionMutedTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedStateBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface3Color)
    )
    .setColor(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedStateTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
    )
    .setSize(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedStateTextSize,
    )
    .setStyle(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedStateTextStyle,
        defValue = Typeface.NORMAL
    )
    .setFont(
        index = R.styleable.ChannelListView_sceytUiChannelListMentionMutedTextFont
    )
    .build()

