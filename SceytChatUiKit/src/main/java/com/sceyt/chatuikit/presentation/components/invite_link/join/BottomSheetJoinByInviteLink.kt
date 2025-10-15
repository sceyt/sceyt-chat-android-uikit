package com.sceyt.chatuikit.presentation.components.invite_link.join

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytBottomSheetJoinByInviteLinkBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.empty
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.extensions.setProgressColor
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.components.invite_link.JoinByInviteLinkResult
import com.sceyt.chatuikit.presentation.components.invite_link.join.adapters.MembersPreviewAdapter
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.invite_link.BottomSheetJoinByInviteLinkStyle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

open class BottomSheetJoinByInviteLink : BottomSheetDialogFragment(), SceytKoinComponent {
    protected val viewModel by viewModel<JoinByInviteLinkViewModel>(
        parameters = {
            parametersOf(requireArguments().parcelable<Uri>(INVITE_LINK_KEY))
        }
    )
    protected lateinit var binding: SceytBottomSheetJoinByInviteLinkBinding
    protected lateinit var style: BottomSheetJoinByInviteLinkStyle
    protected var joinedToChannelListener: ((JoinByInviteLinkResult) -> Unit)? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        style = StyleRegistry.getOrDefault(arguments?.getString(STYLE_ID_KEY)) {
            BottomSheetJoinByInviteLinkStyle.Builder(context, null).build()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SceytAppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SceytBottomSheetJoinByInviteLinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        applyStyle()
        initViews()
        initViewModel()
    }

    protected open fun initViews() {
        binding.btnJoinGroup.setOnClickListener {
            viewModel.joinToChannel()
        }
    }

    protected open fun initViewModel() {
        viewModel.uiState.onEach(::onUiStateChange).launchIn(lifecycleScope)
        viewModel.joinActionState.onEach(::onJoiningStateChange).launchIn(lifecycleScope)
    }

    protected open fun onUiStateChange(state: UiState) {
        when (state) {
            is UiState.Success -> {
                if (checkMaybelAlreadyJoined(channel = state.channel)) {
                    return
                }
                setDetails(state.channel)
                binding.layoutDetails.isVisible = true
                binding.primaryLoading.isVisible = false
            }

            is UiState.Error -> {
                customToastSnackBar(state.error?.message)
            }

            is UiState.Loading -> {
                binding.layoutDetails.isInvisible = true
                binding.primaryLoading.isVisible = true
            }
        }
    }

    protected open fun checkMaybelAlreadyJoined(channel: SceytChannel): Boolean {
        if (channel.userRole.isNullOrBlank()) return false
        dismiss()
        joinedToChannelListener?.invoke(JoinByInviteLinkResult.AlreadyJoined(channel))
        return true
    }

    protected open fun onJoiningStateChange(state: JoinActionState) {
        when (state) {
            is JoinActionState.Joined -> {
                joinedToChannelListener?.invoke(
                    JoinByInviteLinkResult.JoinedByInviteLink(state.channel)
                )
                dismiss()
            }

            is JoinActionState.JoinError -> {
                customToastSnackBar(state.error?.message)
            }

            else -> Unit
        }
        val isJoining = state is JoinActionState.Joining
        binding.loadingJoin.isVisible = isJoining
        binding.btnJoinGroup.text = if (isJoining)
            empty else style.joinButtonText
    }

    protected open fun setDetails(channel: SceytChannel) {
        binding.tvSubject.text = channel.subject
        binding.tvMemberNames.text = buildMemberNames(channel.members.orEmpty())
        binding.avatar.appearanceBuilder()
            .setDefaultAvatar(AvatarView.DefaultAvatar.FromInitials(channel.subject.orEmpty()))
            .setImageUrl(channel.avatarUrl)
            .build().applyToAvatar()
        setMembersAdapter(channel)
    }

    protected open fun setMembersAdapter(channel: SceytChannel) {
        val adapter = MembersPreviewAdapter(memberCount = channel.memberCount.toInt())
        binding.rvMembers.adapter = adapter
        adapter.submitList(channel.members)
    }

    protected open fun buildMemberNames(members: List<SceytMember>): String {
        val names = members.take(3).map { member ->
            SceytChatUIKit.formatters.userNameFormatter.format(
                context = requireContext(),
                from = member.user
            )
        }
        if (names.isEmpty()) return ""

        val displayNames = names.joinToString(", ")
        val remainingCount = members.size - 3

        return if (remainingCount > 0)
            getString(R.string.and_others, displayNames, remainingCount)
        else displayNames
    }

    protected open fun applyStyle() = with(binding) {
        style.backgroundStyle.apply(root)

        // Apply text styles
        style.subjectTextStyle.apply(tvSubject)
        style.subtitleTextStyle.apply(tvInviteTitle)
        style.memberNamesTextStyle.apply(tvMemberNames)

        // Apply button style
        style.joinButtonStyle.apply(btnJoinGroup)
        btnJoinGroup.text = style.joinButtonText

        // Set subtitle text
        tvInviteTitle.text = style.subtitleText

        // Apply progress bar colors
        primaryLoading.setProgressColor(style.primaryProgressBarColor)
        loadingJoin.setProgressColor(style.buttonProgressBarColor)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).isDraggable = false
            }

            setOnCancelListener {
                joinedToChannelListener?.invoke(JoinByInviteLinkResult.Canceled)
            }
        }
    }

    companion object {
        private val TAG = BottomSheetJoinByInviteLink::class.java.simpleName
        private const val STYLE_ID_KEY = "STYLE_ID_KEY"
        private const val INVITE_LINK_KEY = "invite_link"

        fun show(
                fragmentManager: FragmentManager,
                inviteLink: Uri,
                joinedToChannelListener: ((JoinByInviteLinkResult) -> Unit)? = null,
                styleId: String? = null,
        ) {
            val existingSheet = fragmentManager.findFragmentByTag(TAG) as? BottomSheetJoinByInviteLink
            if (existingSheet != null && existingSheet.isAdded) {
                existingSheet.joinedToChannelListener = joinedToChannelListener
                return
            }
            val bottomSheet = BottomSheetJoinByInviteLink().setBundleArguments {
                putString(STYLE_ID_KEY, styleId)
                putParcelable(INVITE_LINK_KEY, inviteLink)
            }
            bottomSheet.joinedToChannelListener = joinedToChannelListener
            bottomSheet.show(fragmentManager, TAG)
        }
    }
}

