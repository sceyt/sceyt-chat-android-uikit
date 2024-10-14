package com.sceyt.chatuikit.styles.common

import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.renderers.VisualRenderer

data class ListItemStyle<TitleFormatter, SubtitleFormatter, AvatarRenderer>(
        val titleTextStyle: TextStyle = TextStyle(),
        val subtitleTextStyle: TextStyle = TextStyle(),
        val avatarStyle: AvatarStyle = AvatarStyle(),
        val titleFormatter: TitleFormatter,
        val subtitleFormatter: SubtitleFormatter,
        val avatarRenderer: AvatarRenderer,
) where TitleFormatter : Formatter<*>,
        SubtitleFormatter : Formatter<*>,
        AvatarRenderer : VisualRenderer



