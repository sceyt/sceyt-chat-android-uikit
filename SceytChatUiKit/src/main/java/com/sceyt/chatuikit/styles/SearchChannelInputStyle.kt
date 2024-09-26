package com.sceyt.chatuikit.styles

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.common.HintStyle
import com.sceyt.chatuikit.styles.common.SearchInputStyle
import com.sceyt.chatuikit.styles.common.TextInputStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

/**
 * Style for the search channel input view.
 * @property backgroundColor Background color for the input, default is [SceytChatUIKitTheme.surface1Color]
 * @property cornerRadius Corner radius for the input, default is 10 dp
 * @property borderWidth Border width for the input, default is 0 dp
 * @property borderColor Border color for the input, default is [SceytChatUIKitTheme.borderColor]
 * @property searchInputStyle Style for the search input.
 * */
data class SearchChannelInputStyle(
        @ColorInt var backgroundColor: Int,
        @Px val cornerRadius: Float,
        @Px val borderWidth: Float ,
        @ColorInt val borderColor: Int,
        val searchInputStyle: SearchInputStyle
) {

    companion object {
        var styleCustomizer = StyleCustomizer<SearchChannelInputStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): SearchChannelInputStyle {
            context.obtainStyledAttributes(attrs, R.styleable.SearchChannelInputView).use { array ->
                val backgroundColor = array.getColor(R.styleable.SearchChannelInputView_sceytUiSearchChannelInputBackgroundColor,
                    context.getCompatColor(SceytChatUIKit.theme.surface1Color))

                val cornerRadius = array.getDimension(R.styleable.SearchChannelInputView_sceytUiSearchChannelInputCornerRadius,
                    dpToPx(10f).toFloat()
                )

                val borderWidth = array.getDimension(R.styleable.SearchChannelInputView_sceytUiSearchChannelInputBorderWidth,
                    0f
                )

                val borderColor = array.getColor(R.styleable.SearchChannelInputView_sceytUiSearchChannelInputBorderColor,
                    context.getCompatColor(SceytChatUIKit.theme.borderColor)
                )

                val textStyle = TextStyle.Builder(array)
                    .setColor(
                        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor)
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

                val hintStyle = HintStyle.Builder(array)
                    .textColor(
                        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputHintTextColor,
                        defValue = context.getCompatColor(SceytChatUIKit.theme.textFootnoteColor)
                    )
                    .hint(
                        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputHintText,
                        defValue = context.getString(R.string.sceyt_search_for_channels)
                    )
                    .build()


                val textInputStyle = TextInputStyle.Builder(array)
                    .setTextStyle(textStyle)
                    .setHintStyle(hintStyle)
                    .build()

                val searchInputStyle = SearchInputStyle.Builder(array)
                    .searchIcon(
                        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputSearchIcon,
                        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_search)?.apply {
                            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                        })
                    .clearIcon(
                        index = R.styleable.SearchChannelInputView_sceytUiSearchChannelInputClearIcon,
                        defValue = context.getCompatDrawable(R.drawable.sceyt_ic_cancel)?.apply {
                            setTint(context.getCompatColor(SceytChatUIKit.theme.iconSecondaryColor))
                        })
                    .textInputStyle(textInputStyle)
                    .build()


                return SearchChannelInputStyle(
                    backgroundColor = backgroundColor,
                    cornerRadius = cornerRadius,
                    borderWidth = borderWidth,
                    borderColor = borderColor,
                    searchInputStyle = searchInputStyle,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}