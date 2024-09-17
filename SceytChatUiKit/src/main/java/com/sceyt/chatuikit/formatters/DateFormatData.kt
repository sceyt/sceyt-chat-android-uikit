package com.sceyt.chatuikit.formatters

data class DateFormatData(
        var format: String? = null,
        var beginTittle: String = "",
        var endTitle: String = ""
) {
    val shouldFormat get() = !format.isNullOrBlank()
    val title get() = beginTittle + endTitle
}