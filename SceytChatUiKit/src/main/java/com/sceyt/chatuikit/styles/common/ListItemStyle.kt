package com.sceyt.chatuikit.styles.common

import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.providers.VisualProvider

data class ListItemStyle<TitleFormatter, SubtitleFormatter, AvatarProvider>(
        val titleTextStyle: String,
        val subtitleTextStyle: String,
        val titleFormatter: TitleFormatter,
        val subtitleFormatter: SubtitleFormatter,
        val avatarProvider: AvatarProvider,
) where TitleFormatter : Formatter<*>,
        SubtitleFormatter : Formatter<*>,
        AvatarProvider : VisualProvider<*, *>



