package com.sceyt.chatuikit.styles.search

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.channel_info.voice.ChannelInfoVoiceItemStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.DateSeparatorStyle
import com.sceyt.chatuikit.styles.common.EmptyStateStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.buildEmptyStateStyle

data class VoiceSearchPageStyle(
    @param:ColorInt val backgroundColor: Int,
    @param:LayoutRes val emptyState: Int,
    val emptyStateStyle: EmptyStateStyle,
    val dateSeparatorStyle: DateSeparatorStyle,
    val voiceItemStyle: ChannelInfoVoiceItemStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<VoiceSearchPageStyle> { _, style -> style }
    }

    internal class Builder(private val context: Context) {
        fun build(): VoiceSearchPageStyle {
            return VoiceSearchPageStyle(
                backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor),
                emptyState = R.layout.sceyt_channel_list_empty_state,
                emptyStateStyle = buildEmptyStateStyle(
                    context = context,
                    iconRes = R.drawable.sceyt_ic_search,
                    titleText = context.getString(R.string.sceyt_ui_channel_list_empty),
                    subtitleText = context.getString(R.string.sceyt_ui_channel_list_empty_desc)
                ),
                dateSeparatorStyle = DateSeparatorStyle(
                    backgroundStyle = BackgroundStyle(
                        backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
                    ),
                    textStyle = TextStyle(
                        color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
                        font = R.font.roboto_medium
                    )
                ),
                voiceItemStyle = ChannelInfoVoiceItemStyle.Builder(context, null).build(),
            ).let { styleCustomizer.apply(context, it) }
        }
    }
}
