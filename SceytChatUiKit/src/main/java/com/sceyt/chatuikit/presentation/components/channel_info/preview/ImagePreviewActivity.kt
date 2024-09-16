package com.sceyt.chatuikit.presentation.components.channel_info.preview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytFragmentPhotoPreviewBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground

class ImagePreviewActivity : AppCompatActivity() {
    private lateinit var binding: SceytFragmentPhotoPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(SceytFragmentPhotoPreviewBinding.inflate(LayoutInflater.from(this)).also {
            binding = it
        }.root)

        statusBarIconsColorWithBackground()
        binding.applyStyle()
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

    private fun SceytFragmentPhotoPreviewBinding.applyStyle() {
        toolbar.setIconsTint(SceytChatUIKit.theme.accentColor)
        toolbar.setTitleColorRes(SceytChatUIKit.theme.textPrimaryColor)
        toolbar.setBackgroundColor(getCompatColor(SceytChatUIKit.theme.primaryColor))
    }

    companion object {
        private var IMAGE_PATH_KEY = "image_path_key"
        private var TOOLBAR_TITLE_KEY = "toolbar_title_key"

        fun launchActivity(context: Context, imagePath: String, toolbarTitle: String?) {
            context.launchActivity<ImagePreviewActivity> {
                putExtra(IMAGE_PATH_KEY, imagePath)
                putExtra(TOOLBAR_TITLE_KEY, toolbarTitle)
            }
        }
    }
}
