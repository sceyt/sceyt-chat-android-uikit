package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.files.ChannelFilesFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.ChannelMembersFragment

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
                is ChannelLinksFragment -> activity.getString(R.string.sceyt_links)
                else -> ""
            }
        } ?: ""
    }

    fun getFragment() = mFragments
}