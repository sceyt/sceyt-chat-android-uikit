package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.files.ChannelFilesFragment
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.links.ChannelLinksFragment
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.ChannelMediaFragment
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.voice.ChannelVoiceFragment

class ViewPagerAdapter(private val activity: AppCompatActivity,
                       private val fragments: List<Fragment>) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return fragments.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }

    fun getTagByPosition(position: Int): String {
        return fragments.getOrNull(position)?.let {
            when (it) {
                is ChannelMediaFragment -> activity.getString(R.string.sceyt_media)
                is ChannelFilesFragment -> activity.getString(R.string.sceyt_files)
                is ChannelLinksFragment -> activity.getString(R.string.sceyt_links)
                is ChannelVoiceFragment -> activity.getString(R.string.sceyt_voice)
                else -> ""
            }
        } ?: ""
    }

    fun getFragment() = fragments

    fun historyCleared() {
        fragments.forEach {
            (it as? HistoryClearedListener)?.onHistoryCleared()
        }
    }

    fun interface HistoryClearedListener{
        fun onHistoryCleared()
    }
}