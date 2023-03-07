package com.sceyt.sceytchatuikit.presentation.uicomponents.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytActivityShareBinding
import com.sceyt.sceytchatuikit.extensions.customToastSnackBar
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.common.SceytLoader
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.sharebaleactivity.SceytShareableActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.share.viewmodel.ShareViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.share.viewmodel.ShareViewModel.State.Finish
import com.sceyt.sceytchatuikit.presentation.uicomponents.share.viewmodel.ShareViewModel.State.Loading
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

open class SceytShareActivity : SceytShareableActivity() {
    private lateinit var binding: SceytActivityShareBinding
    protected val viewModel: ShareViewModel by viewModels()
    private val sharedUris = ArrayList<Uri>()
    private var body: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(SceytActivityShareBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(SceytKitConfig.isDarkMode)

        getDataFromIntent()
        binding.initViews()
    }

    private fun getDataFromIntent() {
        when {
            Intent.ACTION_SEND == intent.action -> {
                if (intent.getParcelableExtra<Parcelable?>(Intent.EXTRA_STREAM) != null) {
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    uri?.let {
                        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        sharedUris.add(uri)
                    }
                } else if (intent.getCharSequenceExtra(Intent.EXTRA_TEXT) != null) {
                    binding.messageInput.isVisible = false
                    body = intent.getCharSequenceExtra(Intent.EXTRA_TEXT) as String
                } else finishSharingAction()
            }
            Intent.ACTION_SEND_MULTIPLE == intent.action -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (uris != null && uris.isNotEmpty()) {
                    if (uris.size > 20)
                        customToastSnackBar(getString(R.string.sceyt_shara_max_item_count))
                    for (uri in uris.take(20)) {
                        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        sharedUris.add(uri)
                    }
                } else finishSharingAction()
            }
            else -> finishSharingAction()
        }
    }

    private fun SceytActivityShareBinding.initViews() {
        determinateShareBtnState()

        toolbar.setNavigationIconClickListener {
            onBackPressed()
        }

        toolbar.setQueryChangeListener(::onSearchQueryChanged)

        btnShare.setOnClickListener {
            onShareClick()
        }
    }

    protected fun sendTextMessage() {
        viewModel.sendTextMessage(channelIds = selectedChannels.toLongArray(), body = body.toString())
            .onEach {
                when (it) {
                    Loading -> SceytLoader.showLoading(this@SceytShareActivity)
                    Finish -> {
                        SceytLoader.hideLoading()
                        finishSharingAction()
                    }
                }
            }.launchIn(lifecycleScope)
    }

    protected fun sendFilesMessage() {
        val messageBody = (binding.messageInput.text ?: "").trim().toString()
        viewModel.sendFilesMessage(channelIds = selectedChannels.toLongArray(), uris = sharedUris, messageBody)
            .onEach {
                when (it) {
                    Loading -> SceytLoader.showLoading(this@SceytShareActivity)
                    Finish -> {
                        SceytLoader.hideLoading()
                        finishSharingAction()
                    }
                }
            }.launchIn(lifecycleScope)
    }

    protected fun determinateShareBtnState() {
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

    override fun getRV() = binding.rvChannels

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
            sharedUris.isNotEmpty() -> {
                sendFilesMessage()
            }
            else -> finishSharingAction()
        }
    }

    companion object {
        fun newIntent(context: Context, intent: Intent): Intent {
            return Intent(context, SceytShareActivity::class.java).apply {
                action = intent.action
                intent.extras?.let { putExtras(it) }
            }
        }
    }
}