package com.sceyt.chatuikit.styles.common

import androidx.annotation.ColorInt
import com.sceyt.chatuikit.formatters.Formatter
import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

data class SelectableListItemStyle<TitleFormatter, SubtitleFormatter, AvatarProvider>(
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        @ColorInt val dividerColor: Int = UNSET_COLOR,
        val checkboxStyle: CheckboxStyle = CheckboxStyle(),
        val titleTextStyle: TextStyle = TextStyle(),
        val subtitleTextStyle: TextStyle = TextStyle(),
        val titleFormatter: TitleFormatter,
        val subtitleFormatter: SubtitleFormatter,
        val avatarProvider: AvatarProvider,
) where  TitleFormatter : Formatter<*>,
         SubtitleFormatter : Formatter<*>,
         AvatarProvider : VisualProvider<*, *>