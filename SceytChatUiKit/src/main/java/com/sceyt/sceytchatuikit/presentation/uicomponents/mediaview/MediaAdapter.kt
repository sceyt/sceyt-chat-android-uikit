package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MediaAdapter(activity: AppCompatActivity, private val mediaFiles: ArrayList<MediaFile>) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int {
        return mediaFiles.size
    }

    override fun createFragment(position: Int): Fragment {
        return MediaFragment.newInstance(mediaFiles[position])
    }
}