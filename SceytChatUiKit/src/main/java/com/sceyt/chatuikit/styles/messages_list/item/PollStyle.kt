package com.sceyt.chatuikit.styles.messages_list.item

import android.content.Context
import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.renderers.AvatarRenderer
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.CheckboxStyle
import com.sceyt.chatuikit.styles.common.TextStyle

data class VoterAvatarRendererAttributes(
        val bubbleBackgroundStyle: BackgroundStyle,
        val voter: SceytUser,
)

data class PollStyle(
        @param:ColorInt val dividerColor: Int,
        @param:ColorInt val progressTrackColor: Int,
        @param:ColorInt val progressColor: Int,
        val questionTextStyle: TextStyle,
        val pollTypeTextStyle: TextStyle,
        val viewResultsTextStyle: TextStyle,
        val viewResultsDisabledTextStyle: TextStyle,
        val optionTextStyle: TextStyle,
        val voteCountTextStyle: TextStyle,
        val checkboxStyle: CheckboxStyle,
        val voterAvatarStyle: AvatarStyle,
        val pollTypeFormatter: Formatter<SceytPollDetails>,
        val voteCountFormatter: Formatter<PollOptionUiModel>,
        val voterAvatarRenderer: AvatarRenderer<VoterAvatarRendererAttributes>,
) {
    companion object {
        var styleCustomizer = StyleCustomizer<PollStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray,
    ) {
        @ColorInt
        private var dividerColor: Int = UNSET_COLOR

        @ColorInt
        private var progressTrackColor: Int = UNSET_COLOR

        @ColorInt
        private var progressColor: Int = UNSET_COLOR
        private var questionTextStyle: TextStyle = TextStyle()
        private var pollTypeTextStyle: TextStyle = TextStyle()
        private var viewResultsTextStyle: TextStyle = TextStyle()
        private var viewResultsDisabledTextStyle: TextStyle = TextStyle()
        private var optionTextStyle: TextStyle = TextStyle()
        private var voteCountTextStyle: TextStyle = TextStyle()
        private var checkboxStyle: CheckboxStyle = CheckboxStyle()
        private var voterAvatarStyle: AvatarStyle = AvatarStyle()

        fun dividerColor(@StyleableRes index: Int, @ColorInt defValue: Int = dividerColor) = apply {
            this.dividerColor = typedArray.getColor(index, defValue)
        }

        fun progressTrackColor(@StyleableRes index: Int, @ColorInt defValue: Int = progressTrackColor) = apply {
            this.progressTrackColor = typedArray.getColor(index, defValue)
        }

        fun progressColor(@StyleableRes index: Int, @ColorInt defValue: Int = progressColor) = apply {
            this.progressColor = typedArray.getColor(index, defValue)
        }

        fun questionTextStyle(questionTextStyle: TextStyle) = apply {
            this.questionTextStyle = questionTextStyle
        }

        fun pollTypeTextStyle(pollTypeTextStyle: TextStyle) = apply {
            this.pollTypeTextStyle = pollTypeTextStyle
        }

        fun viewResultsTextStyle(viewResultsTextStyle: TextStyle) = apply {
            this.viewResultsTextStyle = viewResultsTextStyle
        }

        fun viewResultsDisabledTextStyle(viewResultsDisabledTextStyle: TextStyle) = apply {
            this.viewResultsDisabledTextStyle = viewResultsDisabledTextStyle
        }

        fun optionTextStyle(optionTextStyle: TextStyle) = apply {
            this.optionTextStyle = optionTextStyle
        }

        fun voteCountTextStyle(voteCountTextStyle: TextStyle) = apply {
            this.voteCountTextStyle = voteCountTextStyle
        }

        fun checkboxStyle(checkboxStyle: CheckboxStyle) = apply {
            this.checkboxStyle = checkboxStyle
        }

        fun voterAvatarStyle(voterAvatarStyle: AvatarStyle) = apply {
            this.voterAvatarStyle = voterAvatarStyle
        }

        fun build() = PollStyle(
            dividerColor = dividerColor,
            progressTrackColor = progressTrackColor,
            progressColor = progressColor,
            questionTextStyle = questionTextStyle,
            pollTypeTextStyle = pollTypeTextStyle,
            viewResultsTextStyle = viewResultsTextStyle,
            viewResultsDisabledTextStyle = viewResultsDisabledTextStyle,
            optionTextStyle = optionTextStyle,
            voteCountTextStyle = voteCountTextStyle,
            checkboxStyle = checkboxStyle,
            voterAvatarStyle = voterAvatarStyle,
            pollTypeFormatter = SceytChatUIKit.formatters.pollTypeFormatter,
            voteCountFormatter = SceytChatUIKit.formatters.pollVoteCountFormatter,
            voterAvatarRenderer = SceytChatUIKit.renderers.voterAvatarRenderer
        ).let { styleCustomizer.apply(context, it) }
    }
}

