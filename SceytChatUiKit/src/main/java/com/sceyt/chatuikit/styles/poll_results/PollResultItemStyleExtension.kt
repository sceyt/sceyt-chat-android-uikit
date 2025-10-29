package com.sceyt.chatuikit.styles.poll_results

import android.content.res.TypedArray
import android.util.AttributeSet
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.DividerStyle
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

/* Item Divider Style */
internal fun PollResultItemStyle.Builder.buildItemDividerStyle(
        array: TypedArray,
) = DividerStyle.Builder(array)
    .setColor(
        index = R.styleable.PollResults_sceytUiPollResultsDividerColor,
        defValue = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor)
    )
    .setHeight(
        index = R.styleable.PollResults_sceytUiPollResultsDividerHeight,
        defValue = context.resources.getDimensionPixelSize(R.dimen.sceyt_space_8dp)
    )
    .build()

/* Voter Item Style */
internal fun PollResultItemStyle.Builder.buildVoterItemStyle(
        attrs: AttributeSet?
) = VoterItemStyle.Builder(context, attrs).build()