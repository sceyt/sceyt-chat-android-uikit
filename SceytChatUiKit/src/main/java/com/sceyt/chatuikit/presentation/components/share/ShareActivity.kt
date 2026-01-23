package com.sceyt.chatuikit.presentation.components.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.SceytActivityShareBinding
import com.sceyt.chatuikit.databinding.SceytEmptyStateBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.parcelableArrayList
import com.sceyt.chatuikit.extensions.setSafeOnClickListener
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.common.dialogs.SceytLoader
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.share.viewmodel.ShareViewModel
import com.sceyt.chatuikit.presentation.components.share.viewmodel.ShareViewModel.State.Finish
import com.sceyt.chatuikit.presentation.components.share.viewmodel.ShareViewModel.State.Loading
import com.sceyt.chatuikit.presentation.components.shareable.ShareableActivity
import com.sceyt.chatuikit.presentation.custom_views.PageStateView
import com.sceyt.chatuikit.styles.extensions.common.applyStyle
import com.sceyt.chatuikit.styles.share.ShareStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

open class ShareActivity : ShareableActivity<ShareStyle>() {
    protected lateinit var binding: SceytActivityShareBinding
    protected val viewModel: ShareViewModel by viewModels()
    protected var body: String? = null
    protected val shareUris = mutableSetOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(SceytActivityShareBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        applyInsetsAndWindowColor(binding.root)
        statusBarIconsColorWithBackground()

        getDataFromIntent()
        binding.initViews()
        binding.applyStyle()
    }

    override fun initStyle(): ShareStyle {
        return ShareStyle.Builder(this, null).build()
    }

    protected open fun getDataFromIntent() {
        shareUris.clear()
        when {
            Intent.ACTION_SEND == intent.action -> {
                if (intent.parcelable<Parcelable>(Intent.EXTRA_STREAM) != null) {
                    val uri = intent.parcelable<Uri>(Intent.EXTRA_STREAM)
                    uri?.let {
                        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        shareUris.add(uri)
                    }
                } else if (intent.getCharSequenceExtra(Intent.EXTRA_TEXT) != null) {
                    hideInputOnSharingText()
                    body = intent.getCharSequenceExtra(Intent.EXTRA_TEXT) as String
                } else finishSharingAction()
            }

            Intent.ACTION_SEND_MULTIPLE == intent.action -> {
                val uris = intent.parcelableArrayList<Uri>(Intent.EXTRA_STREAM)
                if (!uris.isNullOrEmpty()) {
                    if (uris.size > 20)
                        customToastSnackBar(getString(R.string.sceyt_shara_max_item_count))
                    for (uri in uris.take(20)) {
                        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        shareUris.add(uri)
                    }
                } else finishSharingAction()
            }

            else -> finishSharingAction()
        }
    }

    protected open fun SceytActivityShareBinding.initViews() {
        determinateShareBtnState()
        setupPageStateView()

        toolbar.setNavigationClickListener {
            finish()
        }

        toolbar.setQueryChangeListener(::onSearchQueryChanged)

        btnShare.setSafeOnClickListener {
            onShareClick()
        }
    }

    protected open fun setupPageStateView() {
        binding.pageStateView.setEmptySearchStateView(
            SceytEmptyStateBinding.inflate(
                layoutInflater, binding.pageStateView, false
            ).also { it.applyStyle(style.emptyStateStyle) }.root
        )
    }

    protected open fun hideInputOnSharingText() {
        binding.messageInput.isVisible = false
    }

    protected open fun sendTextMessage() {
        viewModel.sendTextMessage(channelIds = selectedChannels.toLongArray(), body = body.toString())
            .onEach {
                when (it) {
                    Loading -> SceytLoader.showLoading(this@ShareActivity)
                    Finish -> {
                        SceytLoader.hideLoading()
                        finishSharingAction()
                    }
                }
            }.launchIn(lifecycleScope)
    }

    protected open fun sendFilesMessage() {
        val messageBody = (binding.messageInput.text ?: "").trim().toString()
        viewModel.sendFilesMessage(
            channelIds = selectedChannels.toLongArray(),
            uris = shareUris.toList(),
            messageBody
        ).onEach {
            when (it) {
                Loading -> SceytLoader.showLoading(this@ShareActivity)
                Finish -> {
                    SceytLoader.hideLoading()
                    finishSharingAction()
                }
            }
        }.launchIn(lifecycleScope)
    }

    protected open fun determinateShareBtnState() {
        with(binding.btnShare) {
            if (enableNext()) {
                alpha = 1f
                isEnabled = true
            } else {
                alpha = .5f
                isEnabled = false
            }
        }
    }

    override fun getRV(): RecyclerView? {
        return if (::binding.isInitialized)
            binding.rvChannels
        else null
    }

    override fun getPageStateView(): PageStateView? {
        return if (::binding.isInitialized)
            binding.pageStateView
        else null
    }

    override fun onChannelClick(channelItem: ChannelListItem.ChannelItem): Boolean {
        return super.onChannelClick(channelItem).also {
            determinateShareBtnState()
        }
    }

    protected open fun onShareClick() {
        when {
            body.isNotNullOrBlank() -> {
                sendTextMessage()
            }

            shareUris.isNotEmpty() -> {
                sendFilesMessage()
            }

            else -> finishSharingAction()
        }
    }

    protected open fun SceytActivityShareBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        style.actionButtonStyle.apply(btnShare)
        style.searchToolbarStyle.apply(toolbar)
        style.messageInputStyle.apply(messageInput, null)
    }

    companion object {

        @Suppress("unused")
        fun newIntent(context: Context, intent: Intent): Intent {
            return Intent(context, ShareActivity::class.java).apply {
                action = intent.action
                intent.extras?.let { putExtras(it) }
            }
        }
    }
}