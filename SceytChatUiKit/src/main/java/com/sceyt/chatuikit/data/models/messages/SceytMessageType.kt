package com.sceyt.chatuikit.data.models.messages

import java.util.concurrent.ConcurrentHashMap

abstract class SceytMessageType(val value: String) {

    companion object Registry {
        private val registry = ConcurrentHashMap<String, SceytMessageType>()

        fun register(type: SceytMessageType) {
            registry[type.value] = type
        }

        fun fromString(type: String): SceytMessageType {
            return registry[type] ?: Unsupported(type)
        }
    }

    data object Text : SceytMessageType("text")
    data object Media : SceytMessageType("media")
    data object File : SceytMessageType("file")
    data object Link : SceytMessageType("link")
    data object System : SceytMessageType("system")
    data object Poll : SceytMessageType("poll")
    data class Unsupported(val type: String) : SceytMessageType(type)

    init {
        if (this !is Unsupported) register(this)
    }
}