package com.sceyt.chatuikit.styles.invite_link

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.ButtonStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildJoinButtonStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildJoinSubjectTextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildJoinSubtitleTextStyle
import com.sceyt.chatuikit.styles.extensions.invite_link.buildMemberNamesTextStyle

data class BottomSheetJoinWithInviteLinkStyle(
        val backgroundStyle: BackgroundStyle,
        @param:ColorInt val primaryProgressBarColor: Int,
        @param:ColorInt val buttonProgressBarColor: Int,
        val subtitleText: String,
        val joinButtonText: String,
        val subjectTextStyle: TextStyle,
        val subtitleTextStyle: TextStyle,
        val memberNamesTextStyle: TextStyle,
        val joinButtonStyle: ButtonStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<BottomSheetJoinWithInviteLinkStyle> { _, style -> style }
    }

    internal class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): BottomSheetJoinWithInviteLinkStyle {
            context.obtainStyledAttributes(attrs, R.styleable.JoinWithInviteLink).use { array ->
                val primaryProgressBarColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                val buttonProgressBarColor = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor)
                val subtitleText = context.getString(R.string.group_chat_invite)
                val joinButtonText = context.getString(R.string.join_group)

                return BottomSheetJoinWithInviteLinkStyle(
                    backgroundStyle = buildBackgroundStyle(array),
                    primaryProgressBarColor = primaryProgressBarColor,
                    buttonProgressBarColor = buttonProgressBarColor,
                    subtitleText = subtitleText,
                    joinButtonText = joinButtonText,
                    subjectTextStyle = buildJoinSubjectTextStyle(array),
                    subtitleTextStyle = buildJoinSubtitleTextStyle(array),
                    memberNamesTextStyle = buildMemberNamesTextStyle(array),
                    joinButtonStyle = buildJoinButtonStyle(array)
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}

