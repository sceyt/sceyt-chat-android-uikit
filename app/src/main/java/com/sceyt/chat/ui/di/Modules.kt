package com.sceyt.chat.ui.di

import android.app.Application
import androidx.room.Room
import com.sceyt.chat.ui.data.SceytSharedPreference
import com.sceyt.chat.ui.data.SceytSharedPreferenceImpl
import com.sceyt.chat.ui.data.repositories.*
import com.sceyt.chat.ui.persistence.*
import com.sceyt.chat.ui.persistence.logics.*
import com.sceyt.chat.ui.presentation.mainactivity.profile.viewmodel.ProfileViewModel
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.links.viewmodels.LinksViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModules = module {
    single<SceytSharedPreference> { SceytSharedPreferenceImpl(get()) }
}

fun databaseModule(enableDatabase: Boolean) = module {

    fun provideDatabase(application: Application): SceytDatabase {
        return if (enableDatabase)
            Room.databaseBuilder(application, SceytDatabase::class.java, "sceyt_database")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
        else {
            Room.inMemoryDatabaseBuilder(application, SceytDatabase::class.java).build()
        }
    }
    single { provideDatabase(get()) }
    single { get<SceytDatabase>().channelDao() }
    single { get<SceytDatabase>().userDao() }
    single { get<SceytDatabase>().messageDao() }

    single { PersistenceMiddleWareImpl(get(), get(), get()) }
    factory<PersistenceChanelMiddleWare> { get<PersistenceMiddleWareImpl>() }
    factory<PersistenceMessagesMiddleWare> { get<PersistenceMiddleWareImpl>() }
    factory<PersistenceMembersMiddleWare> { get<PersistenceMiddleWareImpl>() }

    factory<PersistenceChannelLogic> { PersistenceChannelLogicImpl(get(), get(), get(), get()) }
    factory<PersistenceMessagesLogic> { PersistenceMessagesLogicImpl(get(), get()) }
    factory<PersistenceMembersLogic> { PersistenceMembersLogicImpl(get(), get(), get()) }
}

val repositoryModule = module {
    factory<ChannelsRepository> { ChannelsRepositoryImpl() }
    factory<ProfileRepository> { ProfileRepositoryImpl() }
    factory<MessagesRepository> { MessagesRepositoryImpl() }
}

val viewModels = module {
    viewModel { ChannelsViewModel(get(), get()) }
    viewModel { params ->
        MessageListViewModel(get(), get(), get(), get(), params.get(), params.get(), params.get())
    }
    viewModel { ProfileViewModel(get(), get(), get()) }
    viewModel { LinksViewModel(get()) }
    viewModel { ChannelAttachmentsViewModel(get()) }
    viewModel { ChannelMembersViewModel(get()) }
}
