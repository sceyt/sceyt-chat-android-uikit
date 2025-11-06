package com.sceyt.chatuikit.styles.poll_results

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle

/* Toolbar Style */
internal fun PollResultsStyle.Builder.buildToolbarStyle(
        array: TypedArray,
) = ToolbarStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
)

/* Question Text Style */
internal fun PollResultsStyle.Builder.buildQuestionTextStyle(
        array: TypedArray,
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.PollResults_sceytUiPollResultsQuestionTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.PollResults_sceytUiPollResultsQuestionTextSize,
        defValue = context.resources.getDimensionPixelSize(R.dimen.mediumTextSize)
    )
    .setFont(
        index = R.styleable.PollResults_sceytUiPollResultsQuestionTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

/* Poll Type Text Style */
internal fun PollResultsStyle.Builder.buildPollTypeTextStyle(
        array: TypedArray,
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.PollResults_sceytUiPollResultsPollTypeTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.PollResults_sceytUiPollResultsPollTypeTextSize,
        defValue = context.resources.getDimensionPixelSize(R.dimen.smallTextSize)
    )
    .setFont(
        index = R.styleable.PollResults_sceytUiPollResultsPollTypeTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

/* Poll Option Voters Toolbar Style */
internal fun PollResultsStyle.Builder.buildPollOptionVotersToolbarStyle(
        array: TypedArray,
) = ToolbarStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
)

/* Poll Option Voters Style */
internal fun PollResultsStyle.Builder.buildPollOptionVotersStyle(
        array: TypedArray,
        attrs: android.util.AttributeSet?
) = PollOptionVotersStyle.Builder(context, array)
    .backgroundColor(context.getCompatColor(SceytChatUIKit.theme.colors.backgroundColor))
    .toolbarTitle("")
    .toolbarStyle(buildPollOptionVotersToolbarStyle(array))
    .voterItemStyle(VoterItemStyle.Builder(context, attrs).build())
    .initialLoaderColor(context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
    .loadMoreProgressColor(context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
    .build()

/* Header Background Style */
internal fun PollResultsStyle.Builder.buildHeaderBackgroundStyle(
        array: TypedArray,
) = BackgroundStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.PollResults_sceytUiPollResultsHeaderBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .setShape(
        Shape.RoundedCornerShape(
            radius = context.resources.getDimension(R.dimen.sceyt_space_8dp)
        )
    )
    .build()
