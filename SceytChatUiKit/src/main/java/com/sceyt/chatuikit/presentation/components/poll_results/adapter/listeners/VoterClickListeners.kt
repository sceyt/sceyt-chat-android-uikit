package com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem

sealed interface VoterClickListeners {

    fun interface VoterClickListener : VoterClickListeners {
        fun onVoterClick(view: View, item: VoterItem.Voter)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : VoterClickListener
}