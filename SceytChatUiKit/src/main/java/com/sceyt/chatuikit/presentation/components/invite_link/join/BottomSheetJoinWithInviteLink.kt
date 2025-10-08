package com.sceyt.chatuikit.presentation.components.invite_link.join

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.databinding.SceytBottomSheetJoinWithInviteLinkBinding
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.setBundleArguments
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.presentation.components.invite_link.join.adapters.MembersPreviewAdapter
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

open class BottomSheetJoinWithInviteLink : BottomSheetDialogFragment(), SceytKoinComponent {
    protected val viewModel by viewModel<JoinWithInviteLinkViewModel>(
        parameters = {
            parametersOf(requireArguments().getString(INVITE_LINK_KEY))
        }
    )
    protected lateinit var binding: SceytBottomSheetJoinWithInviteLinkBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.SceytAppBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SceytBottomSheetJoinWithInviteLinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initViewModel()
    }

    protected open fun initViews() {
        binding.btnJoinGroup.setOnClickListener {

        }
    }

    protected open fun initViewModel() {
        viewModel.uiState.onEach { state ->
            when (state) {
                is UiState.Success -> {
                    setDetails(state.channel)
                }

                is UiState.Error -> {
                    customToastSnackBar(state.error?.message)
                }

                is UiState.Loading -> {
                    //show loading
                }
            }
        }.launchIn(lifecycleScope)
    }

    protected open fun setDetails(channel: SceytChannel) {
        binding.tvSubject.text = channel.subject
        binding.tvMemberNames.text = buildMemberNames(channel.members.orEmpty())
        binding.avatar.appearanceBuilder()
            .setDefaultAvatar(AvatarView.DefaultAvatar.FromInitials(channel.subject.orEmpty()))
            .setImageUrl(channel.avatarUrl)
            .build().applyToAvatar()
        setMembersAdapter(channel.members.orEmpty())
    }

    protected open fun setMembersAdapter(members: List<SceytMember>) {
        val adapter = MembersPreviewAdapter()
        binding.rvMembers.adapter = adapter
        adapter.submitList(members)
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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                BottomSheetBehavior.from(bottomSheet).isDraggable = false
            }
        }
    }

    companion object {
        const val TAG = "BottomSheetJoinWithInviteLink"
        private const val INVITE_LINK_KEY = "invite_link"

        fun show(
                fragmentManager: FragmentManager,
                inviteLink: String,
        ) {
            val bottomSheet = BottomSheetJoinWithInviteLink().setBundleArguments {
                putString(INVITE_LINK_KEY, inviteLink)
            }
            bottomSheet.show(fragmentManager, TAG)
        }
    }
}

