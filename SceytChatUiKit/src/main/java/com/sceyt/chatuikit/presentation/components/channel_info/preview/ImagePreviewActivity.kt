package com.sceyt.chatuikit.presentation.components.channel_info.preview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.databinding.SceytFragmentPhotoPreviewBinding
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.styles.ImagePreviewStyle

class ImagePreviewActivity : AppCompatActivity() {
    private lateinit var binding: SceytFragmentPhotoPreviewBinding
    private lateinit var style: ImagePreviewStyle
    private var imageUrl: String = ""
    private var toolbarTitle: CharSequence = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = ImagePreviewStyle.Builder(this, null).build()
        setContentView(SceytFragmentPhotoPreviewBinding.inflate(LayoutInflater.from(this)).also {
            binding = it
        }.root)

        statusBarIconsColorWithBackground()
        binding.applyStyle()
        initViews()
        getBundleArguments()
        setDetails()
    }

    private fun initViews() {
        binding.toolbar.setNavigationClickListener {
            finish()
        }
    }

    private fun getBundleArguments() {
        imageUrl = intent.getStringExtra(IMAGE_URL) ?: ""
        toolbarTitle = intent.getCharSequenceExtra(TOOLBAR_TITLE) ?: ""
    }

    private fun setDetails() {
        binding.toolbar.setTitle(toolbarTitle)

        Glide.with(this)
            .load(imageUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imageView)
    }

    private fun SceytFragmentPhotoPreviewBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)
    }

    companion object {
        private const val IMAGE_URL = "image_url"
        private const val TOOLBAR_TITLE = "toolbar_title"

        fun launchActivity(
                context: Context,
                imageUrl: String,
                toolbarTitle: CharSequence
        ) {
            context.launchActivity<ImagePreviewActivity> {
                putExtra(IMAGE_URL, imageUrl)
                putExtra(TOOLBAR_TITLE, toolbarTitle)
            }
        }
    }
}
