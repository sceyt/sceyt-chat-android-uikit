package com.sceyt.chat.ui.persistence.extensions

inline fun <reified T : Enum<T>> Int.toEnum(): T = enumValues<T>()[this]