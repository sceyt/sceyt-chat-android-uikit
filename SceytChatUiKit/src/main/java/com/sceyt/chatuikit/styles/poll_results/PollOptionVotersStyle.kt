package com.sceyt.chatuikit.styles.poll_results

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.ToolbarStyle

/**
 * Style configuration for the Poll Option Voters screen
 */
data class PollOptionVotersStyle(
        @param:ColorInt val backgroundColor: Int,
        val toolbarTitle: String,
        val toolbarStyle: ToolbarStyle,
        val voterItemStyle: VoterItemStyle
) {
    companion object {
        var styleCustomizer = StyleCustomizer<PollOptionVotersStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR
        private var toolbarTitle: String = ""
        private var toolbarStyle: ToolbarStyle = ToolbarStyle()
        private var voterItemStyle: VoterItemStyle? = null

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun backgroundColor(@ColorInt backgroundColor: Int) = apply {
            this.backgroundColor = backgroundColor
        }

        fun toolbarTitle(toolbarTitle: String) = apply {
            this.toolbarTitle = toolbarTitle
        }

        fun toolbarStyle(toolbarStyle: ToolbarStyle) = apply {
            this.toolbarStyle = toolbarStyle
        }

        fun voterItemStyle(voterItemStyle: VoterItemStyle) = apply {
            this.voterItemStyle = voterItemStyle
        }

        fun build() = PollOptionVotersStyle(
            backgroundColor = backgroundColor,
            toolbarTitle = toolbarTitle,
            toolbarStyle = toolbarStyle,
            voterItemStyle = voterItemStyle!!,
        ).let { styleCustomizer.apply(context, it) }
    }
}