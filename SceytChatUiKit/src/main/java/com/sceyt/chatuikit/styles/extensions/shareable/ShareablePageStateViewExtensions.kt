package com.sceyt.chatuikit.styles.extensions.shareable

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytSearchChannelsEmptyStateBinding
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTintColorRes

internal fun SceytSearchChannelsEmptyStateBinding.applyShareableStyle() {
    image.setTintColorRes(SceytChatUIKit.theme.colors.accentColor)
    tvTitle.setTextColorRes(SceytChatUIKit.theme.colors.textPrimaryColor)
    tvDescription.setTextColorRes(SceytChatUIKit.theme.colors.textSecondaryColor)
}