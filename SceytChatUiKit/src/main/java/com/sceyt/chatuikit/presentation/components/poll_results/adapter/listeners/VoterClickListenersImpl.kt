package com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem

open class VoterClickListenersImpl : VoterClickListeners.ClickListeners {
    private var voterClickListener: VoterClickListeners.VoterClickListener? = null

    override fun onVoterClick(view: View, item: VoterItem.Voter) {
        voterClickListener?.onVoterClick(view, item)
    }

    fun setListener(listener: VoterClickListeners) {
        voterClickListener = when (listener) {
            is VoterClickListeners.ClickListeners -> {
                listener
            }
            is VoterClickListeners.VoterClickListener -> {
                listener
            }
        }
    }
}