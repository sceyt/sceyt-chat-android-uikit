package com.sceyt.chatuikit.persistence.database.entity.messages

internal enum class StoredPinScope(val value: Int) {
    Unspecified(0),
    ForMe(1),
    ForAll(2);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: Unspecified
    }
}