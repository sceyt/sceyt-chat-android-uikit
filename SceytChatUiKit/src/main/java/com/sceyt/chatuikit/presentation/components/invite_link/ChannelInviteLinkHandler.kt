package com.sceyt.chatuikit.presentation.components.invite_link

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentManager
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.onError
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.presentation.common.SceytLoader
import com.sceyt.chatuikit.presentation.components.invite_link.join.BottomSheetJoinByInviteLink
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

sealed interface JoinByInviteLinkResult {
    data class JoinedByInviteLink(val channel: SceytChannel) : JoinByInviteLinkResult
    data class AlreadyJoined(val channel: SceytChannel) : JoinByInviteLinkResult
    data class Error(val message: String?) : JoinByInviteLinkResult
    data object Canceled : JoinByInviteLinkResult
}

@Suppress("unused")
open class ChannelInviteLinkHandler(
        private val context: Context
) : SceytKoinComponent {
    private val channelInteractor: ChannelInteractor by inject()

    private var _isLoading = AtomicBoolean(false)
    val isLoading: Boolean get() = _isLoading.get()

    /**
     * Handles the invite link by checking if the user is already a member of the channel.
     * If not, it shows a bottom sheet to join the channel using the invite link.
     *
     * @param fragmentManager The FragmentManager to show the bottom sheet.
     * @param timeout The timeout duration in milliseconds to wait for the connection
     *        to be established. Default is 30 seconds.
     * */
    open suspend operator fun invoke(
            fragmentManager: FragmentManager,
            uri: Uri,
            listener: ((JoinByInviteLinkResult) -> Unit)? = null,
            timeout: Long = 30.seconds.inWholeMilliseconds,
    ) {
        if (!_isLoading.compareAndSet(false, true)) return

        if (uri.host != SceytChatUIKit.config.channelLinkDeepLinkConfig?.host) {
            listener?.invoke(JoinByInviteLinkResult.Error("Invalid host"))
            _isLoading.set(false)
            return
        }

        val key = uri.pathSegments.lastOrNull()
        if (key == null) {
            listener?.invoke(JoinByInviteLinkResult.Error("Invalid invite link"))
            _isLoading.set(false)
            return
        }

        showLoader(context)
        try {
            withTimeout(timeout) {
                ConnectionEventManager.awaitToConnectSceyt()
                channelInteractor.getChannelByInviteKey(key)
                    .onSuccessNotNull { channel ->
                        if (channel.userRole.isNullOrBlank()) {
                            showBottomSheetJoinByInviteLink(
                                inviteLink = uri,
                                fragmentManager = fragmentManager,
                                listener = listener
                            )
                        } else {
                            listener?.invoke(JoinByInviteLinkResult.AlreadyJoined(channel))
                        }
                    }.onError {
                        listener?.invoke(JoinByInviteLinkResult.Error(it?.message))
                    }
            }
        } catch (ex: TimeoutCancellationException) {
            listener?.invoke(JoinByInviteLinkResult.Canceled)
        } catch (ex: Exception) {
            listener?.invoke(JoinByInviteLinkResult.Error(ex.message))
        } finally {
            hideLoader()
            _isLoading.set(false)
        }
    }

    protected open fun showBottomSheetJoinByInviteLink(
            inviteLink: Uri,
            fragmentManager: FragmentManager,
            listener: ((JoinByInviteLinkResult) -> Unit)?,
    ) {
        BottomSheetJoinByInviteLink.show(
            fragmentManager = fragmentManager,
            inviteLink = inviteLink,
            joinedToChannelListener = listener,
        )
    }

    protected open fun showLoader(context: Context) {
        SceytLoader.showLoading(context)
    }

    protected open fun hideLoader() {
        SceytLoader.hideLoading()
    }
}

