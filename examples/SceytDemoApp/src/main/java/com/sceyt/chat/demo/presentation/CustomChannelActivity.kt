package com.sceyt.chat.demo.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.callclient.CallClient
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.manager.channelIdOrNull
import com.sceyt.chat.demo.call.ui.CallActivity
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.presentation.components.channel.header.MessagesListHeaderView
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class CustomChannelActivity : ChannelActivity() {

    private val callManager: CallManager by inject()

    private var pendingCallIsVideo: Boolean = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            initiateCall(pendingCallIsVideo)
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

        val missingPermissions = getMissingPermissions(isVideo)

        if (missingPermissions.isEmpty()) {
            if (!tryToFindChannelCallAndJoin()) {
                initiateCall(isVideo)
            }
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun tryToFindChannelCallAndJoin(): Boolean {
        val channel = viewModel.channel
        val currentCall = CallClient.requireInstance().getOngoingCalls().firstOrNull { call ->
            val callChannelId = call.channelIdOrNull ?: return@firstOrNull false
            callChannelId == channel.id
        } ?: return false
        lifecycleScope.launch {
            callManager.joinCall(currentCall, callPrepared = {
                CallActivity.launchOutgoing(
                    context = this@CustomChannelActivity,
                    userId = channel.id.toString(),
                    isVideo = it.videoCall
                )
            })
        }
        return true
    }

    private fun initiateCall(isVideo: Boolean) {
        val channel = viewModel.channel

        lifecycleScope.launch {
            val result = if (channel.isGroup) {
                callManager.startOutgoingGroupCall(
                    channel = channel,
                    isVideo = isVideo,
                    isCallAgain = false
                ) {
                    CallActivity.launchOutgoing(
                        context = this@CustomChannelActivity,
                        userId = channel.id.toString(),
                        isVideo = isVideo
                    )
                }
            } else {
                val peerUserId = channel.getPeer()?.id
                if (peerUserId == null) {
                    Toast.makeText(
                        this@CustomChannelActivity,
                        "Cannot determine peer user",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                callManager.startOutgoingCall(
                    userId = peerUserId,
                    isVideo = isVideo,
                    isCallAgain = false
                ) {
                    CallActivity.launchOutgoing(
                        context = this@CustomChannelActivity,
                        userId = peerUserId,
                        isVideo = isVideo
                    )
                }
            }

            result.onFailure { error ->
                Toast.makeText(
                    this@CustomChannelActivity,
                    "Failed to start call: ${error.message}",
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

        fun launch(context: Context, channel: SceytChannel) {
            context.launchActivity<CustomChannelActivity>(
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_in_right,
                com.sceyt.chatuikit.R.anim.sceyt_anim_slide_hold
            ) {
                putExtra(CHANNEL, channel)
            }
        }
    }
}
