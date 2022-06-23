package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(activity: AppCompatActivity,
                       private val mFragments: ArrayList<Fragment>) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return mFragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return mFragments[position]
    }
}