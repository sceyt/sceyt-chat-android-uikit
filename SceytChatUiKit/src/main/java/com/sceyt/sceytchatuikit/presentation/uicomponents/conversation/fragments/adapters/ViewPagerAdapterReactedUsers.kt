package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapterReactedUsers(fragment: Fragment,
                                   private val fragments: List<Fragment>) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}