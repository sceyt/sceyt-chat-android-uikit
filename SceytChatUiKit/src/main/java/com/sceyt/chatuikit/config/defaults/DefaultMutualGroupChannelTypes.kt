package com.sceyt.chatuikit.config.defaults

import com.sceyt.chatuikit.config.MutualGroupChannelTypes

data object DefaultMutualGroupChannelTypes : MutualGroupChannelTypes {
    override fun getMutualGroupChannelTypes(): List<String> = listOf("group")
}