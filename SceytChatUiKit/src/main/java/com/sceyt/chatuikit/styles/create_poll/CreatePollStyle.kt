package com.sceyt.chatuikit.styles.create_poll

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.SceytComponentStyle
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.SwitchStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildAddOptionTextStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildOptionInputTextStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildOptionItemBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildOptionsTitleTextStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildParametersSwitchStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildParametersTitleTextStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildQuestionBackgroundStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildQuestionInputTextStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildQuestionTitleTextStyle
import com.sceyt.chatuikit.styles.extensions.create_poll.buildToolbarStyle

data class CreatePollStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val dividerColor: Int,
        val toolbarTitle: String,
        val questionTitle: String,
        val questionHint: String,
        val optionsTitle: String,
        val addOptionTitle: String,
        val parametersTitle: String,
        val anonymousPollTitle: String,
        val multipleVotesTitle: String,
        val retractVotesTitle: String,
        val addOptionIcon: Drawable?,
        val removeOptionIcon: Drawable?,
        val dragIcon: Drawable?,
        val toolbarStyle: ToolbarStyle,
        val questionTitleTextStyle: TextStyle,
        val questionInputTextStyle: TextStyle,
        val optionsTitleTextStyle: TextStyle,
        val optionInputTextStyle: TextStyle,
        val addOptionTextStyle: TextStyle,
        val parametersTitleTextStyle: TextStyle,
        val switchStyle: SwitchStyle,
        val questionBackgroundStyle: BackgroundStyle,
        val optionItemBackgroundStyle: BackgroundStyle,
) : SceytComponentStyle() {

    companion object {
        var styleCustomizer = StyleCustomizer<CreatePollStyle> { _, style -> style }
    }

    class Builder(
            internal val context: Context,
            private val attrs: AttributeSet?,
    ) {
        fun build(): CreatePollStyle {
            context.obtainStyledAttributes(attrs, R.styleable.CreatePoll).use { array ->
                val backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor)
                val dividerColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
                val toolbarTitle = context.getString(R.string.sceyt_create_poll)
                val questionTitle = context.getString(R.string.sceyt_poll_question)
                val questionHint = context.getString(R.string.sceyt_poll_add_question)
                val optionsTitle = context.getString(R.string.sceyt_poll_options)
                val addOptionTitle = context.getString(R.string.sceyt_poll_add_option)
                val parametersTitle = context.getString(R.string.sceyt_poll_parameters)
                val anonymousPollTitle = context.getString(R.string.sceyt_poll_anonymous)
                val multipleVotesTitle = context.getString(R.string.sceyt_poll_multiple_votes)
                val retractVotesTitle = context.getString(R.string.sceyt_poll_can_retract_votes)

                val addOptionIcon = context.getCompatDrawable(R.drawable.sceyt_ic_add)?.applyTint(
                    tintColor = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
                )

                val removeOptionIcon = context.getCompatDrawable(R.drawable.sceyt_ic_close)?.applyTint(
                    tintColor = context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
                )

                val dragIcon = context.getCompatDrawable(R.drawable.sceyt_ic_drag)?.applyTint(
                    tintColor = context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
                )

                return CreatePollStyle(
                    backgroundColor = backgroundColor,
                    dividerColor = dividerColor,
                    toolbarTitle = toolbarTitle,
                    questionTitle = questionTitle,
                    questionHint = questionHint,
                    optionsTitle = optionsTitle,
                    addOptionTitle = addOptionTitle,
                    parametersTitle = parametersTitle,
                    anonymousPollTitle = anonymousPollTitle,
                    multipleVotesTitle = multipleVotesTitle,
                    retractVotesTitle = retractVotesTitle,
                    addOptionIcon = addOptionIcon,
                    removeOptionIcon = removeOptionIcon,
                    dragIcon = dragIcon,
                    toolbarStyle = buildToolbarStyle(array),
                    questionTitleTextStyle = buildQuestionTitleTextStyle(array),
                    questionInputTextStyle = buildQuestionInputTextStyle(array),
                    optionsTitleTextStyle = buildOptionsTitleTextStyle(array),
                    optionInputTextStyle = buildOptionInputTextStyle(array),
                    addOptionTextStyle = buildAddOptionTextStyle(array),
                    parametersTitleTextStyle = buildParametersTitleTextStyle(array),
                    switchStyle = buildParametersSwitchStyle(array),
                    questionBackgroundStyle = buildQuestionBackgroundStyle(array),
                    optionItemBackgroundStyle = buildOptionItemBackgroundStyle(array)
                ).let { styleCustomizer.apply(context, it) }
            }
        }
    }
}

