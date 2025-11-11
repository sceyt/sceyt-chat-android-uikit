package com.sceyt.chatuikit.data.models.messages
import java.util.concurrent.ConcurrentHashMap

abstract class SceytMessageType(val value: String) {
    data object Text : SceytMessageType("text")
    data object Media : SceytMessageType("media")
    data object File : SceytMessageType("file")
    data object Link : SceytMessageType("link")
    data object System : SceytMessageType("system")
    data object Poll : SceytMessageType("poll")
    data class Unsupported(val type: String) : SceytMessageType(type)

    companion object Registry {
        private val registry = ConcurrentHashMap<String, SceytMessageType>()

        fun register(type: SceytMessageType) {
            registry[type.value] = type
        }

        fun fromString(type: String): SceytMessageType {
            return registry[type] ?: Unsupported(type)
        }

        init {
            // Register built-in types
            listOf(Text, Media, File, Link, System, Poll)
        }
    }

    init {
        if (this !is Unsupported) register(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SceytMessageType) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }
}
