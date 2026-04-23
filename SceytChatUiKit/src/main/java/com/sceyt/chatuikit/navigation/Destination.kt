package com.sceyt.chatuikit.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.persistence.logicimpl.message.ChannelId
import com.sceyt.chatuikit.presentation.components.camera.CameraMediaPreviewActivity
import com.sceyt.chatuikit.presentation.components.camera.CameraState
import com.sceyt.chatuikit.presentation.components.camera.CustomCameraActivity
import com.sceyt.chatuikit.presentation.components.channel.messages.ChannelActivity
import com.sceyt.chatuikit.presentation.components.channel.messages.preview.SelfDestructingMediaPreviewActivity
import com.sceyt.chatuikit.presentation.components.channel.messages.preview.SelfDestructingVoiceMessageActivity
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelInfoActivity
import com.sceyt.chatuikit.presentation.components.channel_info.preview.ImagePreviewActivity
import com.sceyt.chatuikit.presentation.components.create_chat.create_channel.CreateChannelActivity
import com.sceyt.chatuikit.presentation.components.create_chat.create_group.CreateGroupActivity
import com.sceyt.chatuikit.presentation.components.create_poll.CreatePollActivity
import com.sceyt.chatuikit.presentation.components.forward.ForwardActivity
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchActivity
import com.sceyt.chatuikit.presentation.components.invite_link.ChannelInviteLinkActivity
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewActivity
import com.sceyt.chatuikit.presentation.components.message_info.MessageInfoActivity
import com.sceyt.chatuikit.presentation.components.poll_results.PollResultsActivity
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersActivity
import com.sceyt.chatuikit.presentation.components.select_users.SelectUsersPageArgs
import com.sceyt.chatuikit.presentation.components.startchat.StartChatActivity
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

sealed class Destination {

    open fun navigate(context: Context) {
        val intent = createIntent(context)
        context.startActivity(intent, createOptions(context).toBundle())
    }

    open fun navigateForResult(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
    ) {
        val intent = createIntent(context)
        launcher.launch(intent, createOptions(context))
    }

    abstract fun createIntent(context: Context): Intent

    protected open fun createOptions(context: Context): ActivityOptionsCompat {
        return ActivityOptionsCompat.makeCustomAnimation(
            context,
            R.anim.sceyt_anim_slide_in_right,
            R.anim.sceyt_anim_slide_hold
        )
    }


    // Destinations

    open class Channel(
        val channel: SceytChannel,
        val targetMessageId: Long? = null,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return ChannelActivity.createIntent(context, channel, targetMessageId)
        }
    }

    open class ChannelInfo(
        val channel: SceytChannel,
        val enableSearchMessages: Boolean = false,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return ChannelInfoActivity.createIntent(context, channel, enableSearchMessages)
        }
    }

    open class StartChat : Destination() {
        override fun createIntent(context: Context): Intent {
            return StartChatActivity.createIntent(context)
        }
    }

    open class GlobalSearch(
        val sourceView: View? = null,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return GlobalSearchActivity.createIntent(context, canUseSharedTransition(context))
        }

        override fun createOptions(context: Context): ActivityOptionsCompat {
            val activity = context as? Activity
            val view = sourceView
            return if (activity != null && view != null) {
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    view,
                    GlobalSearchActivity.SHARED_TRANSITION_NAME
                )
            } else {
                super.createOptions(context)
            }
        }

        private fun canUseSharedTransition(context: Context): Boolean {
            return sourceView != null && context is Activity
        }
    }

    open class SelectUsers(
        val args: SelectUsersPageArgs,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return SelectUsersActivity.createIntent(context, args)
        }
    }

    open class CreateChannel : Destination() {
        override fun createIntent(context: Context): Intent {
            return CreateChannelActivity.createIntent(context)
        }
    }

    open class CreateGroup(
        val members: List<SceytMember>,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return CreateGroupActivity.createIntent(context, members)
        }
    }

    open class MessageInfo(
        val message: SceytMessage,
        val itemStyle: MessageItemStyle,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return MessageInfoActivity.createIntent(context, message, itemStyle)
        }
    }

    open class Forward(
        val messages: List<SceytMessage>,
    ) : Destination() {
        constructor(vararg messages: SceytMessage) : this(messages.toList())

        override fun createIntent(context: Context): Intent {
            return ForwardActivity.createIntent(context, messages)
        }
    }

    open class PollResults(
        val message: SceytMessage,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return PollResultsActivity.createIntent(context, message)
        }
    }

    open class CreatePoll(
        val channelId: ChannelId,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return CreatePollActivity.createIntent(context, channelId)
        }
    }

    open class InviteLink(
        val channel: SceytChannel,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return ChannelInviteLinkActivity.createIntent(context, channel)
        }
    }

    open class ImagePreview(
        val imageUrl: String,
        val toolbarTitle: CharSequence,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return ImagePreviewActivity.createIntent(context, imageUrl, toolbarTitle)
        }
    }

    open class SelfDestructingMediaPreview(
        val message: SceytMessage,
        val attachment: SceytAttachment,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return SelfDestructingMediaPreviewActivity.createIntent(context, message, attachment)
        }
    }

    open class SelfDestructingVoicePreview(
        val message: SceytMessage,
        val attachment: SceytAttachment,
        val styleId: String,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return SelfDestructingVoiceMessageActivity.createIntent(
                context = context,
                message = message,
                attachment = attachment,
                styleId = styleId
            )
        }
    }

    open class MediaPreview(
        val params: MediaPreviewParams,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return when (params) {
                is MediaPreviewParams.SingleAttachment -> {
                    MediaPreviewActivity.createIntent(
                        context = context,
                        attachment = params.attachment,
                        from = params.from,
                        channelId = params.channelId,
                        reversed = params.reversed,
                        showInChatChannel = params.showInChatChannel,
                        launchedWithSharedTransition = canUseSharedTransition(context)
                    )
                }

                is MediaPreviewParams.PreloadedList -> {
                    MediaPreviewActivity.createPreloadedIntent(
                        context = context,
                        items = params.items,
                        initialIndex = params.initialIndex,
                        showInChatChannel = params.showInChatChannel,
                        launchedWithSharedTransition = canUseSharedTransition(context)
                    )
                }
            }
        }

        override fun createOptions(context: Context): ActivityOptionsCompat {
            val activity = context as? Activity
            val sourceView = params.sourceView
            return if (activity != null && sourceView != null) {
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    sourceView,
                    MediaPreviewActivity.SHARED_TRANSITION_NAME
                )
            } else {
                super.createOptions(context)
            }
        }

        private fun canUseSharedTransition(context: Context): Boolean {
            return params.sourceView != null && context is Activity
        }
    }

    open class CameraMediaPreview(
        val filePath: String,
        val isVideo: Boolean,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return CameraMediaPreviewActivity.createIntent(context, filePath, isVideo)
        }
    }

    open class CustomCamera(
        val allowedMode: CameraState.AllowedMode? = null,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return CustomCameraActivity.createIntent(context, allowedMode)
        }
    }

    open class AppRoot(
        private val flags: Int = 0,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setPackage(context.packageName)

            if (flags != 0)
                intent.addFlags(flags)

            return intent
        }

        override fun createOptions(context: Context): ActivityOptionsCompat {
            return ActivityOptionsCompat.makeBasic()
        }
    }
}
