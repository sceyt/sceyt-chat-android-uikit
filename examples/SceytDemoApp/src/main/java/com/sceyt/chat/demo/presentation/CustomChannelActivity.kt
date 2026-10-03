package com.sceyt.chat.demo.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.callclient.CallClient
import com.callclient.call.Call
import com.callclient.call.data.onFailure
import com.sceyt.calluikit.model.CallPhase
import com.sceyt.calluikit.ui.SceytCallUiKit
import com.sceyt.calluikit.ui.navigation.CallDestination
import com.sceyt.calluikit.ui.navigation.navigate
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.call.attachActiveCallBanner
import com.sceyt.chat.demo.call.channelIdOrNull
import com.sceyt.chat.demo.call.toCreateCallOptions
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import kotlinx.coroutines.launch

class CustomChannelActivity : ChannelActivity() {

    private var pendingCallIsVideo: Boolean = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            makeCall(pendingCallIsVideo)
        } else {
            Toast.makeText(
                this,
                "Permissions required for ${if (pendingCallIsVideo) "video" else "audio"} call",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.headerView.setToolbarMenu()
        attachActiveCallBanner(binding.root, binding.headerView.id)
    }

    private fun MessagesListHeaderView.setToolbarMenu() {
        if (viewModel.channel.isSelf) {
            return
        }
        setToolbarMenu(R.menu.menu_conversation, Toolbar.OnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_audio_call -> makeCall(false)
                R.id.action_video_call -> makeCall(true)
            }
            return@OnMenuItemClickListener true
        })
    }

    private fun makeCall(isVideo: Boolean) {
        pendingCallIsVideo = isVideo

        val missingPermissions = getMissingPermissions(findChannelCall()?.videoCall ?: isVideo)

        if (missingPermissions.isEmpty()) {
            initiateCall(isVideo)
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun findChannelCall(): Call? =
        CallClient.requireInstance().getOngoingCalls().firstOrNull {
            it.channelIdOrNull == viewModel.channel.id
        }

    private fun initiateCall(isVideo: Boolean) {
        lifecycleScope.launch {
            val controller = SceytCallUiKit.controller
            val currentCall = findChannelCall()
            val result = if (currentCall != null) {
                val state = controller.callState.value
                if (state.isActive && state.call?.id == currentCall.id) {
                    SceytCallUiKit.navigator.navigate(
                        this@CustomChannelActivity,
                        if (state.phase == CallPhase.Incoming) {
                            CallDestination.Incoming()
                        } else {
                            CallDestination.Ongoing()
                        },
                    )
                    return@launch
                }
                controller.joinCall(currentCall)
            } else {
                val options = viewModel.channel.toCreateCallOptions(
                    isVideo = isVideo,
                    currentUserId = SceytChatUIKit.currentUserId,
                )
                if (options == null) {
                    Toast.makeText(
                        this@CustomChannelActivity,
                        "No remote participants available",
                        Toast.LENGTH_SHORT,
                    ).show()
                    return@launch
                }
                controller.startCall(createCallOptions = options)
            }

            result.onFailure { error ->
                Toast.makeText(
                    this@CustomChannelActivity,
                    "Failed to start or join call: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getMissingPermissions(isVideo: Boolean): List<String> {
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO)

        if (isVideo) {
            required.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        return required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        const val CHANNEL = "CHANNEL"

        fun createIntent(
            context: Context,
            channel: SceytChannel,
            targetMessageId: Long? = null,
        ): Intent = context.createIntent<CustomChannelActivity> {
            putExtra(CHANNEL, channel)
            targetMessageId?.let { putExtra(TARGET_MESSAGE_ID, it) }
        }
    }
}
