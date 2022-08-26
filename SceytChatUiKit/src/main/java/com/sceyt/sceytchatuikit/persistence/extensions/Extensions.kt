package com.sceyt.sceytchatuikit.persistence.extensions

inline fun <reified T : Enum<T>> Int.toEnum(): T = enumValues<T>()[this]