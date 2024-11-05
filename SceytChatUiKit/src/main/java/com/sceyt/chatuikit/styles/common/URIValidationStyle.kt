package com.sceyt.chatuikit.styles.common

import com.sceyt.chatuikit.providers.VisualProvider
import com.sceyt.chatuikit.providers.defaults.URIValidationType

data class URIValidationStyle(
        val successTextStyle: TextStyle,
        val errorTextStyle: TextStyle,
        val messageProvider: VisualProvider<URIValidationType, String>,
)