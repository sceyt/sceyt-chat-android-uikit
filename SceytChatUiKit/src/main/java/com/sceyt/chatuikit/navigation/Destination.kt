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

/**
 * Describes a screen or flow that UIKit can open.
 *
 * Each destination owns the intent and launch options for one UIKit route. Apps that need
 * custom navigation should replace a destination from [SceytChatUIKitNavigator.resolve],
 * usually by subclassing the matching destination and overriding [createIntent] or
 * [createOptions].
 */
sealed class Destination {

    /**
     * Opens this destination as regular one-way navigation.
     */
    fun navigate(context: Context) {
        val intent = createIntent(context)
        context.startActivity(intent, createOptions(context).toBundle())
    }

    /**
     * Opens this destination through the caller's [ActivityResultLauncher].
     *
     * The destination only creates the intent and options. The caller remains responsible
     * for interpreting the result in its launcher callback.
     */
    fun navigateForResult(
        context: Context,
        launcher: ActivityResultLauncher<Intent>,
    ) {
        val intent = createIntent(context)
        launcher.launch(intent, createOptions(context))
    }

    /**
     * Creates the intent for this destination.
     */
    abstract fun createIntent(context: Context): Intent

    /**
     * Creates launch options for this destination.
     *
     * The base implementation applies UIKit's default slide-in-right animation. Override
     * this when a destination needs a shared element transition or no custom animation.
     */
    protected open fun createOptions(context: Context): ActivityOptionsCompat {
        return ActivityOptionsCompat.makeCustomAnimation(
            context,
            R.anim.sceyt_anim_slide_in_right,
            R.anim.sceyt_anim_slide_hold
        )
    }


    // Destinations

    /**
     * Opens a chat channel. Apps commonly replace this destination to show a custom
     * channel activity while preserving UIKit's routing call sites.
     */
    open class Channel(
        val channel: SceytChannel,
        val targetMessageId: Long? = null,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return ChannelActivity.createIntent(context, channel, targetMessageId)
        }
    }

    /**
     * Opens channel details and optionally starts in message search mode.
     */
    open class ChannelInfo(
        val channel: SceytChannel,
        val enableSearchMessages: Boolean = false,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return ChannelInfoActivity.createIntent(context, channel, enableSearchMessages)
        }
    }

    /**
     * Opens the UIKit start-chat entry screen.
     */
    open class StartChat : Destination() {
        override fun createIntent(context: Context): Intent {
            return StartChatActivity.createIntent(context)
        }
    }

    /**
     * Opens global message search.
     *
     * When an Activity context and [sourceView] are available, this destination uses the
     * search shared element transition. Otherwise it falls back to the default UIKit
     * slide-in-right transition.
     */
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

    /**
     * Opens the user selection flow with the supplied page arguments.
     */
    open class SelectUsers(
        val args: SelectUsersPageArgs,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return SelectUsersActivity.createIntent(context, args)
        }
    }

    /**
     * Opens the one-to-one channel creation flow.
     */
    open class CreateChannel : Destination() {
        override fun createIntent(context: Context): Intent {
            return CreateChannelActivity.createIntent(context)
        }
    }

    /**
     * Opens the group creation flow using the selected members.
     */
    open class CreateGroup(
        val members: List<SceytMember>,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return CreateGroupActivity.createIntent(context, members)
        }
    }

    /**
     * Opens delivery/read info for a message.
     */
    open class MessageInfo(
        val message: SceytMessage,
        val itemStyle: MessageItemStyle,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return MessageInfoActivity.createIntent(context, message, itemStyle)
        }
    }

    /**
     * Opens the message forwarding flow.
     */
    open class Forward(
        val messages: List<SceytMessage>,
    ) : Destination() {
        constructor(vararg messages: SceytMessage) : this(messages.toList())

        override fun createIntent(context: Context): Intent {
            return ForwardActivity.createIntent(context, messages)
        }
    }

    /**
     * Opens poll result details for a message.
     */
    open class PollResults(
        val message: SceytMessage,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return PollResultsActivity.createIntent(context, message)
        }
    }

    /**
     * Opens poll creation for the target channel.
     */
    open class CreatePoll(
        val channelId: ChannelId,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return CreatePollActivity.createIntent(context, channelId)
        }
    }

    /**
     * Opens invite link management for a channel.
     */
    open class InviteLink(
        val channel: SceytChannel,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return ChannelInviteLinkActivity.createIntent(context, channel)
        }
    }

    /**
     * Opens a simple image preview screen.
     */
    open class ImagePreview(
        val imageUrl: String,
        val toolbarTitle: CharSequence,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return ImagePreviewActivity.createIntent(context, imageUrl, toolbarTitle)
        }
    }

    /**
     * Opens a self-destructing media attachment preview.
     */
    open class SelfDestructingMediaPreview(
        val message: SceytMessage,
        val attachment: SceytAttachment,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return SelfDestructingMediaPreviewActivity.createIntent(context, message, attachment)
        }
    }

    /**
     * Opens a self-destructing voice attachment preview.
     */
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

    /**
     * Opens media preview for either one attachment or a preloaded media list.
     *
     * Passing a source view enables the current shared element transition path when the
     * context is an Activity; otherwise the default transition is used.
     */
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

    /**
     * Opens the preview screen shown after capturing camera media.
     */
    open class CameraMediaPreview(
        val filePath: String,
        val isVideo: Boolean,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return CameraMediaPreviewActivity.createIntent(context, filePath, isVideo)
        }
    }

    /**
     * Opens UIKit's custom camera screen.
     */
    open class CustomCamera(
        val allowedMode: CameraState.AllowedMode? = null,
    ) : Destination() {
        override fun createIntent(context: Context): Intent {
            return CustomCameraActivity.createIntent(context, allowedMode)
        }
    }

    /**
     * Opens the host app launcher/root activity.
     *
     * UIKit uses this as a safe fallback for task-root exits and notification entry
     * points. Apps can replace it from [SceytChatUIKitNavigator.resolve] when their
     * launcher routing needs custom flags, deep links, or a different activity.
     */
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
