package com.sceyt.chatuikit.presentation.components.channel.input.format

enum class StyleType(val value: String) {
    Bold("bold"),
    Italic("italic"),
    Strikethrough("strikethrough"),
    Monospace("monospace"),
    Underline("underline");
}

fun String.toStyleType(): StyleType? {
    return when (this) {
        StyleType.Bold.value -> StyleType.Bold
        StyleType.Italic.value -> StyleType.Italic
        StyleType.Strikethrough.value -> StyleType.Strikethrough
        StyleType.Monospace.value -> StyleType.Monospace
        StyleType.Underline.value -> StyleType.Underline
        else -> null
    }
}