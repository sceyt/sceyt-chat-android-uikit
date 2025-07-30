package com.sceyt.chatuikit.presentation.style

import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry for managing UI component styles with type safety.
 * Survives configuration changes and prevents memory leaks.
 */
object StyleRegistry {

    /** Thread-safe storage for all registered styles */
    private val styles: ConcurrentHashMap<String, SceytComponentStyle> = ConcurrentHashMap()

    /** Get style by ID, returns null if not found */
    operator fun get(id: String): SceytComponentStyle? = styles[id]

    /** Register style with given ID */
    operator fun set(id: String, style: SceytComponentStyle) {
        styles[id] = style
    }

    /** Get style with type safety, returns null if not found or wrong type */
    inline fun <reified T : SceytComponentStyle> getTyped(id: String?): T? {
        id ?: return null
        return this[id] as? T
    }

    /** Get style with fallback if ID is null/invalid */
    inline fun <reified T : SceytComponentStyle> getOrDefault(
            id: String?,
            defaultProvider: () -> T,
    ): T {
        id ?: return defaultProvider()
        return getTyped(id) ?: defaultProvider()
    }

    /** Get style with type safety, throws exception if not found or wrong type */
    inline fun <reified T : SceytComponentStyle> requireTyped(id: String): T {
        val style = this[id]
                ?: throw IllegalArgumentException("Style with id '$id' not found")
        return style as? T
                ?: throw IllegalArgumentException("Style with id '$id' is ${style::class.simpleName}, expected ${T::class.simpleName}")
    }

    /** Register style (alternative to operator function) */
    fun register(style: SceytComponentStyle, id: String = style.styleId) {
        this[id] = style
    }

    /** Remove styles from registry */
    fun unregister(id: String?): SceytComponentStyle? {
        return styles.remove(id)
    }

    /** Remove style from registry, returns removed style or null */
    fun unregister(vararg ids: String) {
        ids.forEach { id ->
            styles.remove(id)
        }
    }

    /** Clear all registered styles */
    fun clear() = styles.clear()

    /** Get debug information about registered styles */
    fun getDebugInfo(): String {
        val typeGroups = styles.values.groupBy { it::class.simpleName }
        return buildString {
            appendLine("StyleRegistry: ${styles.size} total styles")
            typeGroups.forEach { (type, styles) ->
                appendLine("  $type: ${styles.size}")
            }
        }
    }
}