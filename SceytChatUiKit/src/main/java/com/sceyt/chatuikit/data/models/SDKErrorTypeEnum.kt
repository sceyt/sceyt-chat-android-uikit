package com.sceyt.chatuikit.data.models

enum class SDKErrorTypeEnum(
    val value: String,
    val isResendable: Boolean,
) {
    BadRequest(
        "BadRequest",
        false
    ),
    BadParam(
        "BadParam",
        false
    ),
    NotFound(
        "NotFound",
        false
    ),
    NotAllowed(
        "NotAllowed",
        false
    ),
    TooLargeRequest(
        "TooLargeRequest",
        false
    ),
    InternalError(
        "InternalError",
        true
    ),
    TooManyRequests(
        "TooManyRequests",
        true
    ),
    Authentication(
        "Authentication",
        true
    );

    companion object {

        fun fromValue(value: String?): SDKErrorTypeEnum? {
            value ?: return null
            return entries.find { it.value == value }
        }
    }
}