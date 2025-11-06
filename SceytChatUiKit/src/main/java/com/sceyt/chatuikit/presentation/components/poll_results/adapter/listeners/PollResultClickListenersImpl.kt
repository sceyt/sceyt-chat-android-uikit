package com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners

import android.view.View
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem

open class PollResultClickListenersImpl : PollResultClickListeners.ClickListeners {
    private var showAllClickListener: PollResultClickListeners.ShowAllClickListener? = null
    private var voterClickListener: PollResultClickListeners.VoterClickListener? = null

    override fun onShowAllClick(view: View, item: PollResultItem.PollOptionItem) {
        showAllClickListener?.onShowAllClick(view, item)
    }

    override fun onVoterClick(view: View, item: VoterItem.Voter) {
        voterClickListener?.onVoterClick(view, item)
    }

    fun setListener(listener: PollResultClickListeners) {
        when (listener) {
            is PollResultClickListeners.ClickListeners -> {
                showAllClickListener = listener
                voterClickListener = listener
            }
            is PollResultClickListeners.ShowAllClickListener -> {
                showAllClickListener = listener
            }
            is PollResultClickListeners.VoterClickListener -> {
                voterClickListener = listener
            }
        }
    }
}