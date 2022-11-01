package com.sceyt.sceytchatuikit.persistence.extensions

inline fun <reified T : Enum<T>> Int.toEnum(): T = enumValues<T>()[this]

fun String?.equalsIgnoreNull(other: String?): Boolean {
    return (this?.trim() ?: "") == (other?.trim() ?: "")
}

fun <T> List<T>?.equalsIgnoreNull(other: List<T>?): Boolean {
    return (this ?: listOf()) == (other ?: listOf<T>())
}

inline fun <reified T> Array<T>?.equalsIgnoreNull(other: Array<T>?): Boolean {
    return (this ?: arrayOf()).contentEquals((other ?: arrayOf()))
}

fun <T> List<T>.toArrayList(): ArrayList<T> {
    return try {
        this as ArrayList
    } catch (ex: Exception) {
        ArrayList(this)
    }
}