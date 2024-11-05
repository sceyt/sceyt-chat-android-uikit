package com.sceyt.chatuikit.styles.extensions.search_channel

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.SearchChannelInputStyle
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle

internal fun SearchChannelInputStyle.Builder.buildTextStyle(
        array: TypedArray
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputTextSize
    )
    .setStyle(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputTextStyle
    )
    .setFont(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputTextFont
    )
    .build()

internal fun SearchChannelInputStyle.Builder.buildHintStyle(
        array: TypedArray
) = HintStyle.Builder(array)
    .color(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputHintTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textFootnoteColor)
    )
    .hint(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputHintText,
        defValue = context.getString(R.string.sceyt_search_for_channels)
    )
    .build()

internal fun SearchChannelInputStyle.Builder.buildTextInputStyle(
        array: TypedArray
) = TextInputStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color))
    .setBorderColor(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputBorderColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    )
    .setBorderWidth(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputBorderWidth,
        defValue = 0
    )
    .setCornerRadius(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputCornerRadius,
        defValue = dpToPx(10f).toFloat()
    )
    .setTextStyle(buildTextStyle(array))
    .setHintStyle(buildHintStyle(array))
    .build()

internal fun SearchChannelInputStyle.Builder.buildSearchInputStyle(
        array: TypedArray
) = SearchInputStyle.Builder(array)
    .searchIcon(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputSearchIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_search)?.applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
        )
    )
    .clearIcon(
        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputClearIcon,
        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_cancel)?.applyTint(
            context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor))
    )
    .textInputStyle(buildTextInputStyle(array))
    .build()


