package com.sceyt.chatuikit.styles.poll_results

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.styles.common.AvatarStyle
import com.sceyt.chatuikit.styles.common.TextStyle

/* User Name Text Style */
internal fun VoterItemStyle.Builder.buildUserNameTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
    font = R.font.roboto_medium,
    size = context.resources.getDimensionPixelSize(R.dimen.mediumTextSize)
)

/* Vote Time Text Style */
internal fun VoterItemStyle.Builder.buildVoteTimeTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKit.theme.colors.textSecondaryColor),
    font = R.font.roboto_regular,
    size = context.resources.getDimensionPixelSize(R.dimen.smallTextSize)
)

/* Avatar Style */
internal fun VoterItemStyle.Builder.buildAvatarStyle(
        array: TypedArray,
) = AvatarStyle.Builder(array).build()