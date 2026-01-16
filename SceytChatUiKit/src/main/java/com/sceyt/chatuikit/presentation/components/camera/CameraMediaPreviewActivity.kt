package com.sceyt.chatuikit.presentation.components.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytActivityCameraMediaPreviewBinding
import com.sceyt.chatuikit.extensions.applySystemWindowInsetsPadding
import com.sceyt.chatuikit.styles.camera.CameraMediaPreviewStyle
import java.io.File

class CameraMediaPreviewActivity : AppCompatActivity() {

    private lateinit var binding: SceytActivityCameraMediaPreviewBinding
    private lateinit var style: CameraMediaPreviewStyle
    private var filePath: String? = null
    private var isVideo: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SceytActivityCameraMediaPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        style = CameraMediaPreviewStyle.Builder(this, null).build()
        applyStyle()

        filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        isVideo = intent.getBooleanExtra(EXTRA_IS_VIDEO, false)

        WindowInsetsControllerCompat(window, binding.root).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        binding.root.applySystemWindowInsetsPadding(
            applyTop = true,
            applyRight = true,
            applyLeft = true
        )
        binding.bottomControls.applySystemWindowInsetsPadding(
            applyBottom = true
        )

        setupListeners()
        displayMedia()
    }

    private fun setupListeners() {
        binding.btnRetake.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.btnConfirm.setOnClickListener {
            filePath?.let { path ->
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_RESULT_URI, path)
                    putExtra(EXTRA_RESULT_IS_VIDEO, isVideo)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        binding.btnPlayPause.setOnClickListener {
            if (binding.previewVideo.isPlaying) {
                binding.previewVideo.pause()
                style.playIcon?.let {
                    binding.btnPlayPause.background = it
                    binding.btnPlayPause.setImageDrawable(null)
                }
                binding.btnPlayPause.contentDescription = getString(R.string.sceyt_play)
            } else {
                binding.previewVideo.start()
                style.pauseIcon?.let {
                    binding.btnPlayPause.background = it
                    binding.btnPlayPause.setImageDrawable(null)
                }
                binding.btnPlayPause.contentDescription = getString(R.string.sceyt_pause)
            }
        }
    }

    private fun displayMedia() {
        val path = filePath ?: return

        if (isVideo) {
            binding.previewImage.isVisible = false
            binding.previewVideo.isVisible = true
            binding.btnPlayPause.isVisible = true
            style.playIcon?.let {
                binding.btnPlayPause.background = it
                binding.btnPlayPause.setImageDrawable(null)
            }
            binding.btnPlayPause.contentDescription = getString(R.string.sceyt_play)

            binding.previewVideo.setVideoPath(path)
            binding.previewVideo.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                binding.previewVideo.seekTo(1)
            }
        } else {
            binding.previewVideo.isVisible = false
            binding.previewImage.isVisible = true
            binding.btnPlayPause.isVisible = false
            binding.previewImage.setImageURI(Uri.fromFile(File(path)))
        }
    }

    override fun onPause() {
        super.onPause()
        if (binding.previewVideo.isPlaying) {
            binding.previewVideo.pause()
            style.playIcon?.let {
                binding.btnPlayPause.background = it
                binding.btnPlayPause.setImageDrawable(null)
            }
            binding.btnPlayPause.contentDescription = getString(R.string.sceyt_play)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (binding.previewVideo.isPlaying) {
            binding.previewVideo.stopPlayback()
        }
    }

    private fun applyStyle() {
        binding.root.setBackgroundColor(style.backgroundColor)
        binding.bottomControls.setBackgroundColor(style.bottomControlsBackgroundColor)

        style.retakeIcon?.let {
            binding.btnRetake.background = it
            binding.btnRetake.setImageDrawable(null)
        }
        style.confirmIcon?.let {
            binding.btnConfirm.background = it
            binding.btnConfirm.setImageDrawable(null)
        }
        style.playIcon?.let {
            binding.btnPlayPause.background = it
            binding.btnPlayPause.setImageDrawable(null)
        }
    }

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_IS_VIDEO = "is_video"
        const val EXTRA_RESULT_URI = "result_uri"
        const val EXTRA_RESULT_IS_VIDEO = "result_is_video"

        fun launch(context: Context, filePath: String, isVideo: Boolean) {
            context.startActivity(Intent(context, CameraMediaPreviewActivity::class.java).apply {
                putExtra(EXTRA_FILE_PATH, filePath)
                putExtra(EXTRA_IS_VIDEO, isVideo)
            })
        }
    }
}
