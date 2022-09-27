package com.sceyt.sceytchatuikit.di

import android.content.Context
import androidx.room.Room
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.SceytSharedPreferenceImpl
import com.sceyt.sceytchatuikit.data.repositories.*
import com.sceyt.sceytchatuikit.persistence.*
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogic
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.connectionlogic.PersistenceConnectionLogic
import com.sceyt.sceytchatuikit.persistence.logics.connectionlogic.PersistenceConnectionLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.memberslogic.PersistenceMembersLogic
import com.sceyt.sceytchatuikit.persistence.logics.memberslogic.PersistenceMembersLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogicImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.viewmodels.LinksViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.creategroup.viewmodel.CreateGroupViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

internal val appModules = module {
    single<SceytSharedPreference> { SceytSharedPreferenceImpl(get()) }
}

internal fun databaseModule(enableDatabase: Boolean) = module {

    fun provideDatabase(context: Context): SceytDatabase {
        return if (enableDatabase)
            Room.databaseBuilder(context, SceytDatabase::class.java, "sceyt_database")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build()
        else {
            Room.inMemoryDatabaseBuilder(context, SceytDatabase::class.java).build()
        }
    }
    single { provideDatabase(get()) }
    single { get<SceytDatabase>().channelDao() }
    single { get<SceytDatabase>().userDao() }
    single { get<SceytDatabase>().messageDao() }

    single { PersistenceMiddleWareImpl() }
    factory<PersistenceChanelMiddleWare> { get<PersistenceMiddleWareImpl>() }
    factory<PersistenceMessagesMiddleWare> { get<PersistenceMiddleWareImpl>() }
    factory<PersistenceMembersMiddleWare> { get<PersistenceMiddleWareImpl>() }

    factory<PersistenceChannelsLogic> { PersistenceChannelsLogicImpl(get(), get(), get(), get(), get()) }
    factory<PersistenceMessagesLogic> { PersistenceMessagesLogicImpl(get(), get(), get(), get(), get()) }
    factory<PersistenceMembersLogic> { PersistenceMembersLogicImpl(get(), get(), get()) }
    factory<PersistenceConnectionLogic> { PersistenceConnectionLogicImpl(get()) }
}

internal val repositoryModule = module {
    factory<ChannelsRepository> { ChannelsRepositoryImpl() }
    factory<ProfileRepository> { ProfileRepositoryImpl() }
    factory<MessagesRepository> { MessagesRepositoryImpl() }
}

internal val viewModels = module {
    viewModel { params ->
        MessageListViewModel(params.get(), params.get(), params.get())
    }
    viewModel { LinksViewModel(get()) }
    viewModel { ChannelAttachmentsViewModel(get()) }
    viewModel { ChannelMembersViewModel(get()) }
    viewModel { CreateGroupViewModel() }
}
