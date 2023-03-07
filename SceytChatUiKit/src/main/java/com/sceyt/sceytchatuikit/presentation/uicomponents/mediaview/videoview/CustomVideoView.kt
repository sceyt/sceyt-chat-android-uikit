package com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.videoview

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.sceyt.sceytchatuikit.databinding.SceytVideoViewBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.MediaActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.videoview.VideoControllerView.MediaPlayerControl


class CustomVideoView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), MediaPlayerControl {
    private val binding = SceytVideoViewBinding.inflate(LayoutInflater.from(context), this, true)
    private var currentMediaPath: String? = null

    init {
        binding.controllerView.setAnchorView(binding.root)
        binding.controllerView.player = this
        binding.videoView.onSurfaceUpdated = {
            binding.controllerView.reloadUI()
        }
    }

    private fun initPlayer(mediaPath: String?) {
        if (mediaPath == null) return
        if (binding.videoView.isPlaying) {
            binding.videoView.reset()
        }
        binding.videoView.setDataSource(mediaPath)
    }

    private fun initPlayer(uri: Uri) {
        if (binding.videoView.isPlaying) {
            binding.videoView.reset()
        }
        binding.videoView.setDataSource(context, uri)
    }

    private fun initPlayer(rawId: Int) {
        if (binding.videoView.isPlaying) {
            binding.videoView.reset()
        }
        binding.videoView.setRawData(rawId)
    }

    private fun setInitialState() {
    }

    fun setVideoPath(mediaPath: String?, startPlay: Boolean = false, isLooping: Boolean = false) {
        if (currentMediaPath == mediaPath) return
        currentMediaPath = mediaPath
        initPlayer(mediaPath)
        binding.videoView.prepareAsync {
            if (startPlay)
                binding.videoView.start()
            binding.videoView.isLooping = isLooping
            post {
                binding.controllerView.setUserVisibleHint((context as MediaActivity).isShowMediaDetail())
            }
        }
    }

    fun setVideoRaw(resId: Int, startPlay: Boolean = false, isLooping: Boolean = false) {
        initPlayer(resId)
        if (startPlay) {
            binding.videoView.prepareAsync {
                binding.videoView.start()
                binding.videoView.isLooping = isLooping
                post {
                    binding.controllerView.setUserVisibleHint((context as MediaActivity).isShowMediaDetail())
                }
            }
        }
    }

    fun setVideoUri(uri: Uri, startPlay: Boolean = false, isLooping: Boolean = false) {
        initPlayer(uri)
        if (startPlay) {
            binding.videoView.prepareAsync {
                binding.videoView.start()
                binding.videoView.isLooping = isLooping
                post {
                    binding.controllerView.setUserVisibleHint((context as MediaActivity).isShowMediaDetail())
                }
            }
        }
    }

    fun setLooping(isLooping: Boolean) {
        binding.videoView.isLooping = isLooping
    }

    fun release() {
        binding.videoView.release()
        setInitialState()
    }

    fun hideController() {
        binding.controllerView.hide()
    }

    fun showController() {
        binding.controllerView.show(3000)
    }

    fun showController(isShow: Boolean) {
        if (isShow) {
            binding.controllerView.show()
        } else {
            binding.controllerView.hide()
        }
    }

    fun setUserVisibleHint(isVisibleToUser: Boolean) {
        binding.controllerView.setUserVisibleHint(isVisibleToUser)
    }

    fun setPlayingListener(listener: VideoControllerView.PlayingListener) {
        binding.controllerView.setPlayingListener(listener)
    }

    override fun start() {
        binding.videoView.start()
    }

    override fun pause() {
        binding.videoView.pause()
    }

    val mediaPlayer get() = binding.videoView.mediaPlayer

    override val duration: Int
        get() = binding.videoView.duration

    override val currentPosition: Int
        get() = binding.videoView.currentPosition

    override fun seekTo(pos: Int) {
        binding.videoView.seekTo(pos)
    }

    override val isPlaying: Boolean
        get() = binding.videoView.isPlaying

    override fun canPause(): Boolean = true
}
