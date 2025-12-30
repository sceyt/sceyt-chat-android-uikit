package com.sceyt.chatuikit.styles.extensions.self_destructing_media_preview

import android.content.res.TypedArray
import android.graphics.Color
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.setIconsTintColorRes
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.common.MenuStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.styles.preview.SelfDestructingMediaPreviewStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

internal fun SelfDestructingMediaPreviewStyle.Builder.buildTimelineTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
)

internal fun buildMessageBodyBackgroundStyle(
    array: TypedArray,
) = BackgroundStyle(
    background = android.graphics.drawable.GradientDrawable(
        android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(
            Color.parseColor("#0017191C"), // 0%
            Color.parseColor("#4017191C"), // 25%
            Color.parseColor("#8017191C"), // 50%
            Color.parseColor("#BF17191C")  // 75%
        )
    )
)

internal fun buildMessageBodyTextStyle(
    array: TypedArray,
) = TextStyle(
    color = Color.WHITE
)

internal fun SelfDestructingMediaPreviewStyle.Builder.buildToolbarStyle(
        array: TypedArray,
) = ToolbarStyle(
    backgroundColor = context.getCompatColor(R.color.sceyt_media_primary_color),
    underlineColor = Color.TRANSPARENT,
    titleTextStyle = TextStyle(
        size = context.resources.getDimensionPixelSize(R.dimen.smallTextSize),
        color = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor),
        font = R.font.roboto_medium
    ),
    navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
        context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
    ),
    menuStyle = MenuStyle(
        menuRes = R.menu.sceyt_menu_self_destructing_media_preview,
        menuCustomizer = {
            setIconsTintColorRes(context, SceytChatUIKit.theme.colors.onPrimaryColor)
        })
)