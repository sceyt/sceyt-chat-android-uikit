package com.sceyt.chat.ui.presentation.mainactivity.adapters

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainViewPagerAdapter(activity: AppCompatActivity,
                           private val mFragments: ArrayList<Fragment>,
                           private val mTagsList: ArrayList<String>) : FragmentStateAdapter(activity) {
    private var oldTag: String? = null

    fun addFragment(fragment: Fragment, tag: String) {
        if (!mTagsList.contains(tag)) {
            mTagsList.add(tag)
            mFragments.add(fragment)
        }
        oldTag = tag
    }


    fun getPosition(tag: String): Int {
        return mTagsList.indexOf(tag)
    }

    fun getFragmentByBottomNavPosition(position: Int): Fragment? {
        return if (position < mFragments.size)
            mFragments[position]
        else null
    }

    fun getFragmentByBottomNavTag(tag: String): Fragment? {
        val index = mTagsList.indexOf(tag)
        if (index != -1)
            return mFragments[index]
        return null
    }

    override fun getItemCount(): Int {
        return mFragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return mFragments[position]
    }
}