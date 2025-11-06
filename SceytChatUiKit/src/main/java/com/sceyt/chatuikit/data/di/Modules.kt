package com.sceyt.chatuikit.data.di

import com.sceyt.chatuikit.data.repositories.AttachmentsRepositoryImpl
import com.sceyt.chatuikit.data.repositories.ChannelInviteKeyRepositoryImpl
import com.sceyt.chatuikit.data.repositories.ChannelsRepositoryImpl
import com.sceyt.chatuikit.data.repositories.MessageMarkersRepositoryImpl
import com.sceyt.chatuikit.data.repositories.MessagesRepositoryImpl
import com.sceyt.chatuikit.data.repositories.PollRepositoryImpl
import com.sceyt.chatuikit.data.repositories.ProfileRepositoryImpl
import com.sceyt.chatuikit.data.repositories.ReactionsRepositoryImpl
import com.sceyt.chatuikit.data.repositories.SceytSharedPreferenceImpl
import com.sceyt.chatuikit.data.repositories.UsersRepositoryImpl
import com.sceyt.chatuikit.persistence.repositories.AttachmentsRepository
import com.sceyt.chatuikit.persistence.repositories.ChannelInviteKeyRepository
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import com.sceyt.chatuikit.persistence.repositories.MessageMarkersRepository
import com.sceyt.chatuikit.persistence.repositories.MessagesRepository
import com.sceyt.chatuikit.persistence.repositories.PollRepository
import com.sceyt.chatuikit.persistence.repositories.ProfileRepository
import com.sceyt.chatuikit.persistence.repositories.ReactionsRepository
import com.sceyt.chatuikit.persistence.repositories.SceytSharedPreference
import com.sceyt.chatuikit.persistence.repositories.UsersRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val repositoryModule = module {
    single<SceytSharedPreference> { SceytSharedPreferenceImpl(get()) }
    factory<ChannelsRepository> { ChannelsRepositoryImpl() }
    factory<ProfileRepository> { ProfileRepositoryImpl() }
    factory<MessagesRepository> { MessagesRepositoryImpl() }
    factory<AttachmentsRepository> { AttachmentsRepositoryImpl() }
    factory<ReactionsRepository> { ReactionsRepositoryImpl() }
    factory<PollRepository> { PollRepositoryImpl() }
    factory<UsersRepository> { UsersRepositoryImpl() }
    factory<MessageMarkersRepository> { MessageMarkersRepositoryImpl() }
    factoryOf<ChannelInviteKeyRepository>(::ChannelInviteKeyRepositoryImpl)
}