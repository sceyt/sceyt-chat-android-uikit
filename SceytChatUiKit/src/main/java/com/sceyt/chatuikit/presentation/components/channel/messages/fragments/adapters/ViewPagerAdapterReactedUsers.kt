package com.sceyt.chatuikit.presentation.components.channel.messages.fragments.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sceyt.chatuikit.data.models.messages.SceytReaction

class ViewPagerAdapterReactedUsers(
    fragment: Fragment,
    fragments: List<FragmentReactedUsers>
) : FragmentStateAdapter(fragment) {
    private val fragments = fragments.toMutableList()

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun containsItem(itemId: Long): Boolean {
        return fragments.any { it.getKey().hashCode().toLong() == itemId }
    }

    override fun createFragment(position: Int): FragmentReactedUsers {
        return fragments[position]
    }

    override fun getItemId(position: Int): Long {
        return fragments[position].getKey().hashCode().toLong()
    }

    fun removeFragment(key: String) {
        fragments
            .indexOfFirst { it.getKey() == key }
            .takeIf { it != -1 }?.let { position ->
                fragments.removeAt(position)
                notifyItemRemoved(position)
            }
    }

    fun addOrUpdateItem(fragment: FragmentReactedUsers, reaction: SceytReaction) {
        fragments.find { it.getKey() == reaction.key }?.update() ?: run {
            this.fragments.add(fragment)
            notifyItemInserted(fragments.lastIndex)
        }
    }

    fun updateAllReactionsFragment() {
        fragments.firstOrNull()?.update()
    }
}