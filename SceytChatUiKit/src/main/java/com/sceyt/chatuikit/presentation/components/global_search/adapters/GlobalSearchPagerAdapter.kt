package com.sceyt.chatuikit.presentation.components.global_search.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchTab

open class GlobalSearchPagerAdapter(
    activity: FragmentActivity,
    open val tabs: List<GlobalSearchTab> = GlobalSearchTab.entries.toList(),
    private val fragmentProvider: (GlobalSearchTab) -> Fragment = {
        error("GlobalSearchPagerAdapter requires a fragmentProvider that supplies fragment arguments.")
    },
) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        return createFragment(tabs[position])
    }

    protected open fun createFragment(tab: GlobalSearchTab): Fragment {
        return fragmentProvider(tab)
    }

    open fun positionOf(tab: GlobalSearchTab): Int = tabs.indexOf(tab)

    open fun tabAt(position: Int): GlobalSearchTab = tabs[position]
}