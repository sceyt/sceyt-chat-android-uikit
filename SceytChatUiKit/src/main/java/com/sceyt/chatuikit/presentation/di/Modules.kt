package com.sceyt.chatuikit.presentation.di

import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.viewmodel.ReactionsInfoViewModel
import com.sceyt.chatuikit.presentation.components.channel.messages.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.components.channel_info.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.chatuikit.presentation.components.channel_info.members.viewmodel.ChannelMembersViewModel
import com.sceyt.chatuikit.presentation.components.channel_list.channels.viewmodel.ChannelsViewModel
import com.sceyt.chatuikit.presentation.components.invite_link.ChannelInviteLinkViewModel
import com.sceyt.chatuikit.presentation.components.invite_link.join.JoinWithInviteLinkViewModel
import com.sceyt.chatuikit.presentation.components.invite_link.shareqr.ShareInviteQrViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val ChannelInfoMediaViewModelQualifier = named("ChannelInfoMediaFragment")
val ChannelInfoFilesViewModelQualifier = named("ChannelInfoFilesFragment")
val ChannelInfoLinksViewModelQualifier = named("ChannelInfoLinksFragment")
val ChannelInfoVoiceViewModelQualifier = named("ChannelInfoVoiceFragment")

internal val viewModelModule = module {
    viewModel { params ->
        MessageListViewModel(params.get(), params.get(), params.get())
    }
    viewModel { params ->
        ChannelsViewModel(params.get())
    }
    viewModel { parameters -> ChannelMembersViewModel(channelId = parameters.get(), get(), get()) }
    viewModel(qualifier = ChannelInfoMediaViewModelQualifier) { ChannelAttachmentsViewModel(get(), get(), get()) }
    viewModel(qualifier = ChannelInfoFilesViewModelQualifier) { ChannelAttachmentsViewModel(get(), get(), get()) }
    viewModel(qualifier = ChannelInfoLinksViewModelQualifier) { ChannelAttachmentsViewModel(get(), get(), get()) }
    viewModel(qualifier = ChannelInfoVoiceViewModelQualifier) { ChannelAttachmentsViewModel(get(), get(), get()) }
    viewModel { parameters -> ReactionsInfoViewModel(get(), messageId = parameters.get(), key = parameters.get()) }
    viewModel { params -> ShareInviteQrViewModel(get(), linkQrData = params.get()) }
    viewModel { params -> ChannelInviteLinkViewModel(params.get()) }
    viewModel { params -> JoinWithInviteLinkViewModel(params.get(), get()) }
}

