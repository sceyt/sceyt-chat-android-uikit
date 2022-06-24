package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.files.ChannelFilesFragment
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment

class ViewPagerAdapter(private val activity: AppCompatActivity,
                       private val mFragments: ArrayList<Fragment>) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return mFragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return mFragments[position]
    }

    fun getTagByPosition(position: Int): String {
        return mFragments.getOrNull(position)?.let {
            when (it) {
                is ChannelMembersFragment -> activity.getString(R.string.sceyt_members)
                is ChannelMediaFragment -> activity.getString(R.string.sceyt_media)
                is ChannelFilesFragment -> activity.getString(R.string.sceyt_files)
                else -> ""
            }
        } ?: ""
    }
}