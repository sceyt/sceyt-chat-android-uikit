package com.sceyt.chatuikit.config

data class ChannelURIConfig(
        val prefix: String = "@",
        val minLength: Int = 5,
        val maxLength: Int = 50,
        val regex: Regex = Regex("^[a-zA-Z0-9_]*\$")
)