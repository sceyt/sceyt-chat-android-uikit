package com.sceyt.chatuikit.presentation.components.channel.input.providers

/**
 * Extension function to combine this provider with another provider.
 */
operator fun InputActionsProvider.plus(other: InputActionsProvider): CompositeInputActionsProvider {
    return when {
        this is CompositeInputActionsProvider && other is CompositeInputActionsProvider -> {
            // Both are composite, merge them
            CompositeInputActionsProvider().apply {
                add(this@plus)
                add(other)
            }
        }
        this is CompositeInputActionsProvider -> {
            // This is composite, add other to it
            this.add(other)
            this
        }
        other is CompositeInputActionsProvider -> {
            // Other is composite, add this to it
            other.add(this)
            other
        }
        else -> {
            // Neither is composite, create new one
            CompositeInputActionsProvider(this, other)
        }
    }
}

/**
 * Creates a composite provider from multiple providers.
 */
@Suppress("unused")
fun compositeOf(vararg providers: InputActionsProvider): CompositeInputActionsProvider {
    return CompositeInputActionsProvider(*providers)
}

