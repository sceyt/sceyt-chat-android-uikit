package com.sceyt.chatuikit.config

data class MemberRolesConfig(
    val owner: String = "owner",
    val admin: String = "admin",
    val participant: String = "participant",
    val subscriber: String = "subscriber"
)
