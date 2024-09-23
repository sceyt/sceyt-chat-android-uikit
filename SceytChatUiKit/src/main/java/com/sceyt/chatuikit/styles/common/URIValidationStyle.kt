package com.sceyt.chatuikit.styles.common

import com.sceyt.chatuikit.providers.Provider
import com.sceyt.chatuikit.providers.defaults.URIValidationType

data class URIValidationStyle(
        val successTextStyle: TextStyle,
        val errorTextStyle: TextStyle,
        val messageProvider: Provider<URIValidationType, String>
)
