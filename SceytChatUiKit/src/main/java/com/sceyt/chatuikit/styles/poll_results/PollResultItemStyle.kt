package com.sceyt.chatuikit.styles.poll_results

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.TextStyle

data class PollResultItemStyle(
        val optionNameTextStyle: TextStyle,
        val voteCountTextStyle: TextStyle,
        val showAllButtonTextStyle: TextStyle,
        val optionBackgroundStyle: BackgroundStyle,
        val voteCountFormatter: Formatter<Int>,
        val voterItemStyle: VoterItemStyle
) {
    companion object {
        var styleCustomizer = com.sceyt.chatuikit.styles.StyleCustomizer<PollResultItemStyle> { _, style -> style }
    }

    class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): PollResultItemStyle {
            return context.obtainStyledAttributes(attrs, R.styleable.PollResults).use { array ->
                PollResultItemStyle(
                    optionNameTextStyle = buildOptionNameTextStyle(array),
                    voteCountTextStyle = buildVoteCountTextStyle(array),
                    showAllButtonTextStyle = buildShowAllButtonTextStyle(array),
                    optionBackgroundStyle = buildOptionBackgroundStyle(array),
                    voteCountFormatter = SceytChatUIKit.formatters.pollResultVoteCountFormatter,
                    voterItemStyle = buildVoterItemStyle(attrs)
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}