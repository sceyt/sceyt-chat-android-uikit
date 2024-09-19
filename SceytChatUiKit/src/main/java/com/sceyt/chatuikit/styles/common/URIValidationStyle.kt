package com.sceyt.chatuikit.styles.common

import com.sceyt.chatuikit.providers.ChannelURIValidationMessageProvider

data class URIValidationStyle(
        val successTextStyle: TextStyle,
        val errorTextStyle: TextStyle,
        val messageProvider: ChannelURIValidationMessageProvider
)
