package com.sceyt.chatuikit.styles.common

import androidx.annotation.StyleRes
import com.sceyt.chatuikit.styles.StyleConstants

data class MenuStyle(
        @StyleRes val style: Int = StyleConstants.UNSET_STYLE,
        @StyleRes val titleAppearance: Int = StyleConstants.UNSET_STYLE
)
