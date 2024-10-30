package com.sceyt.chatuikit.presentation.components.channel_info.preview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytFragmentPhotoPreviewBinding
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.styles.ImagePreviewStyle

class ImagePreviewActivity : AppCompatActivity() {
    private lateinit var binding: SceytFragmentPhotoPreviewBinding
    private lateinit var style: ImagePreviewStyle
    private lateinit var channel: SceytChannel

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
        channel = requireNotNull(intent?.parcelable(CHANNEL))
    }

    private fun setDetails() {
        binding.toolbar.setTitle(style.channelNameFormatter.format(this, channel))

        Glide.with(this)
            .load(channel.iconUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.imageView)
    }

    private fun SceytFragmentPhotoPreviewBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(toolbar)
    }

    companion object {
        private var CHANNEL = "channel"

        fun launchActivity(context: Context, channel: SceytChannel) {
            context.launchActivity<ImagePreviewActivity> {
                putExtra(CHANNEL, channel)
            }
        }
    }
}
