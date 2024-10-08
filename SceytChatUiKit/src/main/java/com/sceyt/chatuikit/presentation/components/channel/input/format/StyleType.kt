package com.sceyt.chatuikit.presentation.components.channel.input.format

enum class StyleType {
    Bold, Italic, Strikethrough, Monospace, Underline;
}

fun String.toStyleType(): StyleType? {
    return when (this) {
        "bold" -> StyleType.Bold
        "italic" -> StyleType.Italic
        "strikethrough" -> StyleType.Strikethrough
        "monospace" -> StyleType.Monospace
        "underline" -> StyleType.Underline
        else -> null
    }
}