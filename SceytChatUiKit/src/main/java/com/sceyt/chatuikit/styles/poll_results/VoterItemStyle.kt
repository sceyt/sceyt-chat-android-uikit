package com.sceyt.chatuikit.styles.poll_results

import android.content.Context
import android.util.AttributeSet
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.UserFormatter
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle

data class VoterItemStyle(
        val userNameTextStyle: TextStyle,
        val voteTimeTextStyle: TextStyle,
        val avatarStyle: AvatarStyle,
        val userNameFormatter: UserFormatter,
        val avatarRenderer: AvatarRenderer<SceytUser>,
        val voteCountFormatter: com.sceyt.chatuikit.formatters.Formatter<Int>
) {

    companion object {
        var styleCustomizer = com.sceyt.chatuikit.styles.StyleCustomizer<VoterItemStyle> { _, style -> style }
    }

    class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): VoterItemStyle {
            return context.obtainStyledAttributes(attrs, intArrayOf()).use { array ->
                VoterItemStyle(
                    userNameTextStyle = buildUserNameTextStyle(array),
                    voteTimeTextStyle = buildVoteTimeTextStyle(array),
                    avatarStyle = buildAvatarStyle(array),
                    userNameFormatter = SceytChatUIKit.formatters.userNameFormatter,
                    avatarRenderer = SceytChatUIKit.renderers.userAvatarRenderer,
                    voteCountFormatter = SceytChatUIKit.formatters.pollResultVoteCountFormatter,
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}