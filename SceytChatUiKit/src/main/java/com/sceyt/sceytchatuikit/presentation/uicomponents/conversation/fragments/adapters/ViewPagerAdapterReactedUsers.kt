package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sceyt.chat.models.message.Reaction
import com.sceyt.sceytchatuikit.extensions.findIndexed

class ViewPagerAdapterReactedUsers(fragment: Fragment,
                                   private val fragments: ArrayList<FragmentReactedUsers>) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): FragmentReactedUsers {
        return fragments[position]
    }

    override fun getItemId(position: Int): Long {
        return fragments[position].hashCode().toLong()
    }

    fun removeFragment(key: String) {
        fragments.findIndexed { it.getKey() == key }?.let { (position, _) ->
            fragments.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun addOrUpdateItem(fragment: FragmentReactedUsers, reaction: Reaction) {
        fragments.findIndexed { it.getKey() == reaction.key }?.second?.update() ?: run {
            this.fragments.add(fragment)
            notifyItemInserted(fragments.lastIndex)
        }
    }
}