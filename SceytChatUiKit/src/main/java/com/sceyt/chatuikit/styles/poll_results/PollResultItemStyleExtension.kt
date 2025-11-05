package com.sceyt.chatuikit.styles.poll_results

import android.content.res.TypedArray
import android.util.AttributeSet
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.Shape
import com.sceyt.chatuikit.styles.common.TextStyle

/* Option Name Text Style */
internal fun PollResultItemStyle.Builder.buildOptionNameTextStyle(
    array: TypedArray,
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.PollResults_sceytUiPollResultsOptionNameTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor)
    )
    .setSize(
        index = R.styleable.PollResults_sceytUiPollResultsOptionNameTextSize,
        defValue = context.resources.getDimensionPixelSize(R.dimen.mediumTextSize)
    )
    .setFont(
        index = R.styleable.PollResults_sceytUiPollResultsOptionNameTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

/* Vote Count Text Style */
internal fun PollResultItemStyle.Builder.buildVoteCountTextStyle(
    array: TypedArray,
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.PollResults_sceytUiPollResultsVoteCountTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor)
    )
    .setSize(
        index = R.styleable.PollResults_sceytUiPollResultsVoteCountTextSize,
        defValue = context.resources.getDimensionPixelSize(R.dimen.smallTextSize)
    )
    .setFont(
        index = R.styleable.PollResults_sceytUiPollResultsVoteCountTextFont,
        defValue = R.font.roboto_regular
    )
    .build()

/* Option Background Style */
internal fun PollResultItemStyle.Builder.buildOptionBackgroundStyle(
    array: TypedArray,
) = BackgroundStyle.Builder(array)
    .setBackgroundColor(
        index = R.styleable.PollResults_sceytUiPollResultsOptionBackgroundColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.surface1Color)
    )
    .setShape(
        Shape.RoundedCornerShape(
            radius = context.resources.getDimension(R.dimen.sceyt_space_8dp)
        )
    )
    .build()

/* Show All Button Text Style */
internal fun PollResultItemStyle.Builder.buildShowAllButtonTextStyle(
    array: TypedArray,
) = TextStyle.Builder(array)
    .setColor(
        index = R.styleable.PollResults_sceytUiPollResultsShowAllButtonTextColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    )
    .setSize(
        index = R.styleable.PollResults_sceytUiPollResultsShowAllButtonTextSize,
        defValue = context.resources.getDimensionPixelSize(R.dimen.mediumTextSize)
    )
    .setFont(
        index = R.styleable.PollResults_sceytUiPollResultsShowAllButtonTextFont,
        defValue = R.font.roboto_medium
    )
    .build()

/* Voter Item Style */
internal fun PollResultItemStyle.Builder.buildVoterItemStyle(
    attrs: AttributeSet?
) = VoterItemStyle.Builder(context, attrs).build()