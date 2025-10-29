package com.sceyt.chatuikit.presentation.components.poll_results

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.DividerStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.poll_results.PollOptionVotersStyle
import com.sceyt.chatuikit.styles.poll_results.PollResultItemStyle
import com.sceyt.chatuikit.styles.poll_results.buildHeaderDividerStyle
import com.sceyt.chatuikit.styles.poll_results.buildPollOptionVotersStyle
import com.sceyt.chatuikit.styles.poll_results.buildPollTypeTextStyle
import com.sceyt.chatuikit.styles.poll_results.buildQuestionTextStyle
import com.sceyt.chatuikit.styles.poll_results.buildToolbarStyle

class PollResultsStyle(
        @param:ColorInt val backgroundColor: Int,
        val toolbarTitle: String,
        val toolbarStyle: ToolbarStyle,
        val questionTextStyle: TextStyle,
        val pollTypeTextStyle: TextStyle,
        val pollTypeFormatter: Formatter<SceytPollDetails>,
        val headerDividerStyle: DividerStyle,
        val pollResultItemStyle: PollResultItemStyle,
        val pollOptionVotersStyle: PollOptionVotersStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<PollResultsStyle> { _, style -> style }
    }

    class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?
    ) {
        fun build(): PollResultsStyle {
            context.obtainStyledAttributes(attrs, R.styleable.PollResults).use { array ->
                val pollResultItemStyle = PollResultItemStyle.Builder(context, attrs).build()

                return PollResultsStyle(
                    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor),
                    toolbarTitle = context.getString(R.string.poll_results),
                    toolbarStyle = buildToolbarStyle(array),
                    questionTextStyle = buildQuestionTextStyle(array),
                    pollTypeTextStyle = buildPollTypeTextStyle(array),
                    headerDividerStyle = buildHeaderDividerStyle(array),
                    pollResultItemStyle = pollResultItemStyle,
                    pollOptionVotersStyle = buildPollOptionVotersStyle(array, attrs),
                    pollTypeFormatter = SceytChatUIKit.formatters.pollTypeFormatter,
                    ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}