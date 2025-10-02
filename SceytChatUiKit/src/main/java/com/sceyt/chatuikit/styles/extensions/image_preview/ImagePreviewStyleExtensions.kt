@file:Suppress("UNUSED_PARAMETER")

package com.sceyt.chatuikit.styles.extensions.image_preview

import android.content.res.TypedArray
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.styles.preview.ImagePreviewStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle

internal fun ImagePreviewStyle.Builder.buildToolbarStyle(
        array: TypedArray,
) = ToolbarStyle(
    backgroundColor = context.getCompatColor(SceytChatUIKit.theme.colors.primaryColor),
    underlineColor = context.getCompatColor(SceytChatUIKit.theme.colors.borderColor),
    navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
        context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
    ),
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.textPrimaryColor),
        font = R.font.roboto_medium
    )
)