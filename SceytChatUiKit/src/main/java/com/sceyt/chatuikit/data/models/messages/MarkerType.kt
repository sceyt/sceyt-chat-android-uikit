package com.sceyt.chatuikit.data.models.messages

sealed class MarkerType(open val value: String) {
    data object Displayed : MarkerType("displayed")
    data object Received : MarkerType("received")
    data object Played : MarkerType("played")
    data class Custom(override val value: String) : MarkerType(value)
}