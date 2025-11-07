package com.sceyt.chatuikit.presentation.di

import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.viewmodel.ReactionsInfoViewModel
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.components.channel_info.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.chatuikit.presentation.components.channel_info.members.viewmodel.ChannelMembersViewModel
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.ChannelsViewModel
import com.sceyt.chatuikit.presentation.components.create_poll.CreatePollViewModel
import com.sceyt.chatuikit.presentation.components.invite_link.ChannelInviteLinkViewModel
import com.sceyt.chatuikit.presentation.components.invite_link.join.JoinByInviteLinkViewModel
import com.sceyt.chatuikit.presentation.components.invite_link.shareqr.ShareInviteQRViewModel
import com.sceyt.chatuikit.presentation.components.poll_results.PollResultsViewModel
import com.sceyt.chatuikit.presentation.components.poll_results.option_voters.PollOptionVotersViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val ChannelInfoMediaViewModelQualifier = named("ChannelInfoMediaFragment")
val ChannelInfoFilesViewModelQualifier = named("ChannelInfoFilesFragment")
val ChannelInfoLinksViewModelQualifier = named("ChannelInfoLinksFragment")
val ChannelInfoVoiceViewModelQualifier = named("ChannelInfoVoiceFragment")

internal val viewModelModule = module {
    viewModelOf(constructor = ::ChannelAttachmentsViewModel, options = {
        qualifier = ChannelInfoMediaViewModelQualifier
    })
    viewModelOf(constructor = ::ChannelAttachmentsViewModel, options = {
        qualifier = ChannelInfoFilesViewModelQualifier
    })
    viewModelOf(constructor = ::ChannelAttachmentsViewModel, options = {
        qualifier = ChannelInfoLinksViewModelQualifier
    })
    viewModelOf(constructor = ::ChannelAttachmentsViewModel, options = {
        qualifier = ChannelInfoVoiceViewModelQualifier
    })

    viewModelOf(::MessageListViewModel)
    viewModelOf(::ChannelsViewModel)
    viewModelOf(::ChannelMembersViewModel)
    viewModelOf(::ReactionsInfoViewModel)
    viewModelOf(::ShareInviteQRViewModel)
    viewModelOf(::ChannelInviteLinkViewModel)
    viewModelOf(::JoinByInviteLinkViewModel)
    viewModelOf(::PollResultsViewModel)
    viewModelOf(::PollOptionVotersViewModel)
    viewModelOf(::CreatePollViewModel)
}