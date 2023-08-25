package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.photopreview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.databinding.SceytFragmentPhotoPreviewBinding
import com.sceyt.sceytchatuikit.extensions.launchActivity

class SceytPhotoPreviewActivity : AppCompatActivity() {
    private lateinit var binding: SceytFragmentPhotoPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytFragmentPhotoPreviewBinding.inflate(LayoutInflater.from(this)).also {
            binding = it
        }.root)

        initViews()
        getBundleArguments()
    }

    private fun initViews() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.root.post {
            WindowInsetsControllerCompat(window, binding.root).apply {
                show(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        binding.icBack.setOnClickListener {
            finish()
        }
    }

    private fun getBundleArguments() {
        val imagePath = requireNotNull(intent?.getStringExtra(IMAGE_PATH_KEY))

        Glide.with(this)
            .load(imagePath)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imageView)
    }

    companion object {
        private var IMAGE_PATH_KEY = "image_path_key"

        fun launchActivity(context: Context, imagePath: String) {
            context.launchActivity<SceytPhotoPreviewActivity> {
                putExtra(IMAGE_PATH_KEY, imagePath)
            }
        }
    }
}