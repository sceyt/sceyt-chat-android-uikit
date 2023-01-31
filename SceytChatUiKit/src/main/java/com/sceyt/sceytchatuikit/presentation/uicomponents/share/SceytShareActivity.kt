package com.sceyt.sceytchatuikit.presentation.uicomponents.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytActivityShareBinding
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.common.SceytLoader
import com.sceyt.sceytchatuikit.presentation.uicomponents.share.viewmodel.ShareActivityViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.share.viewmodel.ShareActivityViewModel.State.*
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

open class SceytShareActivity : SceytShareableActivity() {
    private lateinit var binding: SceytActivityShareBinding
    protected val viewModel: ShareActivityViewModel by viewModels()
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
                    body = intent.getCharSequenceExtra(Intent.EXTRA_TEXT) as String
                } else finish()
            }
            Intent.ACTION_SEND_MULTIPLE == intent.action -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (uris != null && uris.isNotEmpty()) {
                    for (uri in uris) {
                        grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        sharedUris.add(uri)
                    }
                } else finish()
            }
            else -> finish()
        }
    }

    private fun SceytActivityShareBinding.initViews() {
        checkEnableShare()

        toolbar.setNavigationIconClickListener {
            onBackPressed()
        }

        btnShare.setOnClickListener {
            when {
                body.isNotNullOrBlank() -> {
                    sendTextMessage()
                }
                sharedUris.isNotEmpty() -> {
                    sendFilesMessage()
                }
                else -> finish()
            }
        }
    }

    protected fun sendTextMessage() {
        viewModel.sendTextMessage(channelIds = selectedChannels.map {
            it.id
        }.toLongArray(), body = body.toString()).onEach {
            when (it) {
                Loading -> SceytLoader.showLoading(this@SceytShareActivity)
                Finish -> {
                    SceytLoader.hideLoading()
                    finish()
                }
            }
        }.launchIn(lifecycleScope)
    }


    protected fun sendFilesMessage() {
        viewModel.sendFilesMessage(channelIds = selectedChannels.map {
            it.id
        }.toLongArray(), uris = sharedUris).onEach {
            when (it) {
                Loading -> SceytLoader.showLoading(this@SceytShareActivity)
                Finish -> {
                    SceytLoader.hideLoading()
                    finish()
                }
            }
        }.launchIn(lifecycleScope)
    }

    private fun checkEnableShare() {
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

    override fun onChannelsClick(channel: SceytChannel) {
        super.onChannelsClick(channel)
        checkEnableShare()
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