package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.databinding.FragmentMediaBinding

class MediaFragment : Fragment() {
    private lateinit var binding: FragmentMediaBinding
    private var mediaFile: MediaFile? = null
    private var onMediaClickCallback: OnMediaClickCallback? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View {
        return FragmentMediaBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getArgumentData()
        mediaFile?.let {
            if (it.type == FileType.Video) {
                binding.videoView.visibility = View.VISIBLE
                binding.imageView.visibility = View.GONE
                binding.videoView.setVideoPath(it.path, true, true)

            } else {
                binding.imageView.visibility = View.VISIBLE
                binding.videoView.visibility = View.GONE
                Glide.with(this)
                    .load(it.path)
                    .centerInside()
                    .into(binding.imageView)

                binding.imageView.setOnClickListener { onMediaClickCallback?.onMediaClick() }
            }
        }
    }

    private fun getArgumentData() {
        mediaFile = arguments?.getSerializable(MEDIA_FILE) as MediaFile
    }

    override fun onResume() {
        super.onResume()
        setPlayingState(true)
    }

    override fun onPause() {
        super.onPause()
        setPlayingState(false)
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        setPlayingState(isVisibleToUser)
    }

    private fun setPlayingState(isVisibleToUser: Boolean) {
        if (mediaFile?.type == FileType.Video) {
            if (::binding.isInitialized) {
                if (!isVisibleToUser) {
                    if (binding.videoView.isPlaying) {
                        binding.videoView.pause()
                    }
                    binding.videoView.setUserVisibleHint(false)
                } else {
                    binding.videoView.setUserVisibleHint((requireActivity() as MediaActivity).isShowMediaDetail())
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onMediaClickCallback = context as OnMediaClickCallback
    }

    companion object {
        const val MEDIA_FILE = "MEDIA_FILE"

        fun newInstance(mediaFile: MediaFile): MediaFragment {
            val args = Bundle()
            args.putSerializable(MEDIA_FILE, mediaFile)
            val fragment = MediaFragment()
            fragment.arguments = args
            return fragment
        }
    }

}
