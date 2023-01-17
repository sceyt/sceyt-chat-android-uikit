package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class MediaAdapter(fm: FragmentManager, private val mediaFiles: ArrayList<MediaFile>) : FragmentStatePagerAdapter(fm) {

    override fun getCount(): Int {
        return mediaFiles.size
    }

    override fun getItem(position: Int): Fragment {
        return MediaFragment.newInstance(mediaFiles[position])
    }

}