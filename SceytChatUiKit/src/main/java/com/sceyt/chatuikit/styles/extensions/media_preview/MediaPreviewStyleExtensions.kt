@file:Suppress("UNUSED_PARAMETER", "UnusedReceiverParameter")

package com.sceyt.chatuikit.styles.extensions.media_preview

import android.content.res.TypedArray
import android.graphics.Color
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.setIconsTintColorRes
import com.sceyt.chatuikit.styles.MediaPreviewStyle
import com.sceyt.chatuikit.styles.common.MediaLoaderStyle
import com.sceyt.chatuikit.styles.common.MenuStyle
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.common.ToolbarStyle
import com.sceyt.chatuikit.theme.SceytChatUIKitTheme

internal fun MediaPreviewStyle.Builder.buildTimelineTextStyle(
        array: TypedArray,
) = TextStyle(
    color = context.getCompatColor(SceytChatUIKitTheme.colors.primaryColor)
)

internal fun MediaPreviewStyle.Builder.buildToolbarStyle(
        array: TypedArray,
) = ToolbarStyle(
    backgroundColor = context.getCompatColor(R.color.sceyt_media_primary_color),
    underlineColor = Color.TRANSPARENT,
    titleTextStyle = TextStyle(
        color = context.getCompatColor(SceytChatUIKit.theme.colors.onPrimaryColor),
        font = R.font.roboto_medium
    ),
    navigationIcon = context.getCompatDrawable(R.drawable.sceyt_ic_arrow_back).applyTint(
        context.getCompatColor(SceytChatUIKitTheme.colors.onPrimaryColor)
    ),
    menuStyle = MenuStyle(
        menuRes = R.menu.sceyt_menu_media_preview,
        menuCustomizer = {
            setIconsTintColorRes(context, SceytChatUIKit.theme.colors.onPrimaryColor)
        })
)

internal fun MediaPreviewStyle.Builder.buildMediaLoaderStyle(
        array: TypedArray,
) = MediaLoaderStyle()