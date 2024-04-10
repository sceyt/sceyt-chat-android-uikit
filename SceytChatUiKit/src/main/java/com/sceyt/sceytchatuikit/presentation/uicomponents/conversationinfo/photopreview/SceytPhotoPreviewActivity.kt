package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.photopreview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.sceytchatuikit.databinding.SceytFragmentPhotoPreviewBinding
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class SceytPhotoPreviewActivity : AppCompatActivity() {
    private lateinit var binding: SceytFragmentPhotoPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytFragmentPhotoPreviewBinding.inflate(LayoutInflater.from(this)).also {
            binding = it
        }.root)

        statusBarIconsColorWithBackground()
        binding.setupStyle()
        initViews()
        getBundleArguments()
    }

    private fun initViews() {
        binding.toolbar.navigationIcon.setOnClickListener {
            finish()
        }
    }

    private fun getBundleArguments() {
        val imagePath = requireNotNull(intent?.getStringExtra(IMAGE_PATH_KEY))
        val toolbarTitle = intent?.getStringExtra(TOOLBAR_TITLE_KEY)
        binding.toolbar.setTitle(toolbarTitle)

        Glide.with(this)
            .load(imagePath)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imageView)
    }

    private fun SceytFragmentPhotoPreviewBinding.setupStyle() {
        toolbar.setIconsTint(SceytKitConfig.sceytColorAccent)
    }

    companion object {
        private var IMAGE_PATH_KEY = "image_path_key"
        private var TOOLBAR_TITLE_KEY = "toolbar_title_key"

        fun launchActivity(context: Context, imagePath: String, toolbarTitle: String?) {
            context.launchActivity<SceytPhotoPreviewActivity> {
                putExtra(IMAGE_PATH_KEY, imagePath)
                putExtra(TOOLBAR_TITLE_KEY, toolbarTitle)
            }
        }
    }
}
