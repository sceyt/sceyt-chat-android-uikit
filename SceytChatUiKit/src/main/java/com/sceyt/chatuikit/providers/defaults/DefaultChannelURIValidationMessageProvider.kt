package com.sceyt.chatuikit.providers.defaults

import android.content.Context
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.providers.VisualProvider

class DefaultChannelURIValidationMessageProvider : VisualProvider<URIValidationType, String> {
    override fun provide(context: Context, from: URIValidationType): String {
        return when (from) {
            URIValidationType.FreeToUse -> context.getString(R.string.sceyt_valid_url_title)
            URIValidationType.AlreadyTaken -> context.getString(R.string.sceyt_the_url_exist_title)
            URIValidationType.TooShort, URIValidationType.TooLong -> context.getString(R.string.sceyt_url_length_validation_text)
            URIValidationType.InvalidCharacters -> context.getString(R.string.sceyt_url_characters_validation_text)
        }
    }
}

enum class URIValidationType {
    FreeToUse,
    AlreadyTaken,
    TooShort,
    TooLong,
    InvalidCharacters
}