package com.sceyt.chatuikit.config

data class ChannelTypesConfig(
    val direct: String = "direct",
    val group: String = "group",
    val broadcast: String = "broadcast",
) {

    fun getDiscoverableTypes(): List<String> = listOf(broadcast)

    fun getPrivateTypes(): List<String> = listOf(direct, group)
}
