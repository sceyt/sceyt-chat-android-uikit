package com.sceyt.chatuikit.styles.extensions.messages_list_header

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.MessagesListHeaderStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.MenuStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle

internal fun MessagesListHeaderStyle.Builder.buildTitleTextStyle(
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
    .setSize(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderTitleTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderTitleTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderTitleTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

internal fun MessagesListHeaderStyle.Builder.buildSubTitleTextStyle(
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSubTitleTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor))
    .setSize(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSubTitleTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSubTitleTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSubTitleTextFont
    )
    .build()

internal fun MessagesListHeaderStyle.Builder.buildSearchInputHintStyle(
        typedArray: TypedArray
) = HintStyle.Builder(typedArray)
    .textColor(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputHintTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor))
    .hint(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputHintText,
        defValue = context.getString(R.string.sceyt_search)
    )
    .build()

internal fun MessagesListHeaderStyle.Builder.buildSearchInputSearchTextStyle(
        typedArray: TypedArray
) = TextStyle.Builder(typedArray)
    .setColor(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor))
    .setSize(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputTextSize
    )
    .setStyle(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputTextStyle
    )
    .setFont(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputTextFont
    )
    .build()

internal fun MessagesListHeaderStyle.Builder.buildSearchInputTextInputStyle(
        typedArray: TypedArray
) = TextInputStyle.Builder(typedArray)
    .setBackgroundColor(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color))
    .setBorderColor(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputBorderColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    )
    .setBorderWidth(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputBorderWidth,
        defValue = 0
    )
    .setCornerRadius(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputCornerRadius,
        defValue = dpToPx(10f).toFloat()
    )
    .setTextStyle(buildSearchInputSearchTextStyle(typedArray))
    .setHintStyle(buildSearchInputHintStyle(typedArray))
    .build()


internal fun MessagesListHeaderStyle.Builder.buildSearchInputTextStyle(
        typedArray: TypedArray
) = SearchInputStyle.Builder(typedArray)
    .searchIcon(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputSearchIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_search)?.applyTint(
            context, SceytChatUIKit.theme.colors.iconSecondaryColor
        )
    )
    .clearIcon(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderSearchInputClearIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_cancel)?.applyTint(
            context, SceytChatUIKit.theme.colors.iconSecondaryColor
        )
    )
    .textInputStyle(buildSearchInputTextInputStyle(typedArray))
    .build()


@Suppress("UnusedReceiverParameter")
internal fun MessagesListHeaderStyle.Builder.buildMessageActionsMenuStyle(
        typedArray: TypedArray
) = MenuStyle.Builder(typedArray)
    .style(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderMessageActionsMenuStyle
    )
    .titleAppearance(
        index = R.styleable.MessagesListHeaderView_sceytUiMessagesListHeaderMessageActionsMenuTitleAppearance
    )
    .build()