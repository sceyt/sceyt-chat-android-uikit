package com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem

sealed interface PollResultClickListeners {

    fun interface ShowAllClickListener : PollResultClickListeners {
        fun onShowAllClick(view: View, item: PollResultItem.PollOptionItem)
    }

    fun interface VoterClickListener : PollResultClickListeners {
        fun onVoterClick(view: View, item: VoterItem.Voter)
    }

    /** Use this if you want to implement all callbacks */
    interface ClickListeners : ShowAllClickListener, VoterClickListener
}