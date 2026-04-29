package com.sceyt.chatuikit.data.models

enum class SDKErrorTypeEnum(
    val value: String,
    val isResendable: Boolean,
) {
    BadRequest(
        value = "BadRequest",
        isResendable = false
    ),
    BadParam(
        value = "BadParam",
        isResendable = false
    ),
    NotFound(
        value = "NotFound",
        isResendable = false
    ),
    NotAllowed(
        value = "NotAllowed",
        isResendable = false
    ),
    TooLargeRequest(
        value = "TooLargeRequest",
        isResendable = false
    ),
    InternalError(
        value = "InternalError",
        isResendable = true
    ),
    TooManyRequests(
        value = "TooManyRequests",
        isResendable = true
    ),
    Authentication(
        value = "Authentication",
        isResendable = true
    );

    companion object {

        fun fromValue(value: String?): SDKErrorTypeEnum? {
            value ?: return null
            return entries.find { it.value == value }
        }
    }
}