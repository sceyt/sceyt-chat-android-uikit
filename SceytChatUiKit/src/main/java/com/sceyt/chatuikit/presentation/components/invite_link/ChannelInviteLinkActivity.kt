package com.sceyt.chatuikit.presentation.components.invite_link

import android.content.Context
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytActivityChannelInviteLinkBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.overrideTransitions
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity.Companion.CHANNEL
import com.sceyt.chatuikit.presentation.components.channel_info.links.ChannelInfoLinksFragment
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.invite_link.ChannelInviteLinStyle

open class ChannelInviteLinkActivity : AppCompatActivity() {
    protected lateinit var binding: SceytActivityChannelInviteLinkBinding
    protected lateinit var style: ChannelInviteLinStyle
    private lateinit var channel: SceytChannel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        style = ChannelInviteLinStyle.Builder(this, null).build()
        StyleRegistry.register(style)

        enableEdgeToEdge()
        binding = SceytActivityChannelInviteLinkBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsAndWindowColor(binding.root)

        getDataFromIntent()
        loadChannelInviteLinkFragment()
        applyStyle()
        initViews()
    }

    protected open fun getDataFromIntent() {
        channel = requireNotNull(intent?.parcelable(ChannelInfoLinksFragment.Companion.CHANNEL))
    }

    protected open fun initViews() = with(binding) {
        binding.toolbar.setNavigationClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    protected open fun loadChannelInviteLinkFragment() {
        val fragment = ChannelInviteLinkFragment.newInstance(
            styleId = style.styleId,
            channel = channel
        )
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()
    }

    protected open fun applyStyle() = with(binding) {
        root.setBackgroundColor(style.backgroundColor)
        style.toolbarStyle.apply(binding.toolbar)
        toolbar.setTitle(style.toolbarTitle)
    }

    override fun finish() {
        super.finish()
        overrideTransitions(R.anim.sceyt_anim_slide_hold, R.anim.sceyt_anim_slide_out_right, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        StyleRegistry.unregister(intent.getStringExtra(KEY_INVITE_LINK_STYLE_ID))
    }

    companion object {
        private const val KEY_INVITE_LINK_STYLE_ID = "key_invite_link_style_id"

        fun launch(
                context: Context,
                channel: SceytChannel,
        ) {
            context.launchActivity<ChannelInviteLinkActivity>(
                enterAnimResId = R.anim.sceyt_anim_slide_in_right,
                exitAnimResId = R.anim.sceyt_anim_slide_hold,
            ) {
                putExtra(CHANNEL, channel)
            }
        }
    }
}