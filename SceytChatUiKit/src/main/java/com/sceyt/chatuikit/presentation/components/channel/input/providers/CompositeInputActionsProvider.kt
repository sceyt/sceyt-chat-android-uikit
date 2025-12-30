package com.sceyt.chatuikit.presentation.components.channel.input.providers

import android.content.Context
import com.sceyt.chatuikit.data.models.channels.InputAction
import com.sceyt.chatuikit.presentation.components.channel.input.data.InputState

/**
 * Composite provider that combines multiple InputActionsProvider implementations.
 * Actions from all providers are merged together.
 */
class CompositeInputActionsProvider(
    vararg providers: InputActionsProvider
) : InputActionsProvider {
    
    private val providers = mutableListOf<InputActionsProvider>()
    
    init {
        this.providers.addAll(providers)
    }
    
    /**
     * Adds a provider to the composite.
     * @return this instance for chaining
     */
    fun add(provider: InputActionsProvider): CompositeInputActionsProvider {
        providers.add(provider)
        return this
    }
    
    /**
     * Removes a provider from the composite.
     * @return this instance for chaining
     */
    fun remove(provider: InputActionsProvider): CompositeInputActionsProvider {
        providers.remove(provider)
        return this
    }
    
    /**
     * Clears all providers.
     */
    fun clear() {
        providers.clear()
    }
    
    override fun getActions(context: Context, inputState: InputState): List<InputAction> {
        return providers.flatMap { it.getActions(context, inputState) }
    }
    
    override fun onActionClick(action: InputAction) {
        // Notify all providers about the click
        providers.forEach { it.onActionClick(action) }
    }
}

