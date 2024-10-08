package com.sceyt.chatuikit.styles.common

import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.providers.VisualProvider

data class ListItemStyle<TitleFormatter, SubtitleFormatter, AvatarProvider>(
        val titleTextStyle: TextStyle = TextStyle(),
        val subtitleTextStyle: TextStyle = TextStyle(),
        val titleFormatter: TitleFormatter,
        val subtitleFormatter: SubtitleFormatter,
        val avatarProvider: AvatarProvider,
) where TitleFormatter : Formatter<*>,
        SubtitleFormatter : Formatter<*>,
        AvatarProvider : VisualProvider<*, *>



