package com.sceyt.chatuikit.di

import android.content.Context
import androidx.room.Room
import com.sceyt.chatuikit.BuildConfig
import com.sceyt.chatuikit.SceytSyncManager
import com.sceyt.chatuikit.data.SceytSharedPreference
import com.sceyt.chatuikit.data.SceytSharedPreferenceImpl
import com.sceyt.chatuikit.data.repositories.AttachmentsRepository
import com.sceyt.chatuikit.data.repositories.AttachmentsRepositoryImpl
import com.sceyt.chatuikit.data.repositories.ChannelsRepository
import com.sceyt.chatuikit.data.repositories.ChannelsRepositoryImpl
import com.sceyt.chatuikit.data.repositories.MessageMarkersRepository
import com.sceyt.chatuikit.data.repositories.MessageMarkersRepositoryImpl
import com.sceyt.chatuikit.data.repositories.MessagesRepository
import com.sceyt.chatuikit.data.repositories.MessagesRepositoryImpl
import com.sceyt.chatuikit.data.repositories.ProfileRepository
import com.sceyt.chatuikit.data.repositories.ProfileRepositoryImpl
import com.sceyt.chatuikit.data.repositories.ReactionsRepository
import com.sceyt.chatuikit.data.repositories.ReactionsRepositoryImpl
import com.sceyt.chatuikit.data.repositories.UsersRepository
import com.sceyt.chatuikit.data.repositories.UsersRepositoryImpl
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.PersistenceAttachmentsMiddleWare
import com.sceyt.chatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.chatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.chatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.chatuikit.persistence.PersistenceMiddleWareImpl
import com.sceyt.chatuikit.persistence.PersistenceReactionsMiddleWare
import com.sceyt.chatuikit.persistence.PersistenceUsersMiddleWare
import com.sceyt.chatuikit.persistence.SceytDatabase
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferServiceImpl
import com.sceyt.chatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogicImpl
import com.sceyt.chatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.chatuikit.persistence.logics.channelslogic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logics.channelslogic.PersistenceChannelsLogicImpl
import com.sceyt.chatuikit.persistence.logics.connectionlogic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.logics.connectionlogic.PersistenceConnectionLogicImpl
import com.sceyt.chatuikit.persistence.logics.filetransferlogic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logics.filetransferlogic.FileTransferLogicImpl
import com.sceyt.chatuikit.persistence.logics.memberslogic.PersistenceMembersLogic
import com.sceyt.chatuikit.persistence.logics.memberslogic.PersistenceMembersLogicImpl
import com.sceyt.chatuikit.persistence.logics.messageslogic.AttachmentsCache
import com.sceyt.chatuikit.persistence.logics.messageslogic.MessageLoadRangeUpdater
import com.sceyt.chatuikit.persistence.logics.messageslogic.MessagesCache
import com.sceyt.chatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logics.messageslogic.PersistenceMessagesLogicImpl
import com.sceyt.chatuikit.persistence.logics.reactionslogic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logics.reactionslogic.PersistenceReactionsLogicImpl
import com.sceyt.chatuikit.persistence.logics.userslogic.PersistenceUsersLogic
import com.sceyt.chatuikit.persistence.logics.userslogic.PersistenceUsersLogicImpl
import com.sceyt.chatuikit.presentation.uicomponents.conversation.fragments.viewmodels.ReactionsInfoViewModel
import com.sceyt.chatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

internal val appModules = module {
    single<SceytSharedPreference> { SceytSharedPreferenceImpl(get()) }
    single { SceytSyncManager(get(), get(), get()) }
    single<FileTransferService> { FileTransferServiceImpl(get(), get()) }
    single<MessageLoadRangeUpdater> { MessageLoadRangeUpdater(get()) }
}

internal fun databaseModule(enableDatabase: Boolean) = module {

    fun provideDatabase(context: Context): SceytDatabase {
        val builder = if (enableDatabase)
            Room.databaseBuilder(context, SceytDatabase::class.java, "sceyt_ui_kit_database")
        else
            Room.inMemoryDatabaseBuilder(context, SceytDatabase::class.java)

        return builder
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }

    single { provideDatabase(get()) }
    single { get<SceytDatabase>().channelDao() }
    single { get<SceytDatabase>().messageDao() }
    single { get<SceytDatabase>().attachmentsDao() }
    single { get<SceytDatabase>().draftMessageDao() }
    single { get<SceytDatabase>().membersDao() }
    single { get<SceytDatabase>().userDao() }
    single { get<SceytDatabase>().reactionDao() }
    single { get<SceytDatabase>().channelUsersReactionDao() }
    single { get<SceytDatabase>().pendingMarkersDao() }
    single { get<SceytDatabase>().pendingReactionDao() }
    single { get<SceytDatabase>().pendingMessageStateDao() }
    single { get<SceytDatabase>().fileChecksumDao() }
    single { get<SceytDatabase>().linkDao() }
    single { get<SceytDatabase>().loadRangeDao() }

    single { PersistenceMiddleWareImpl(get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceChanelMiddleWare> { get<PersistenceMiddleWareImpl>() }
    single<PersistenceMessagesMiddleWare> { get<PersistenceMiddleWareImpl>() }
    single<PersistenceAttachmentsMiddleWare> { get<PersistenceMiddleWareImpl>() }
    single<PersistenceReactionsMiddleWare> { get<PersistenceMiddleWareImpl>() }
    single<PersistenceMembersMiddleWare> { get<PersistenceMiddleWareImpl>() }
    single<PersistenceUsersMiddleWare> { get<PersistenceMiddleWareImpl>() }

    single<PersistenceChannelsLogic> { PersistenceChannelsLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceMessagesLogic> { PersistenceMessagesLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceAttachmentLogic> { PersistenceAttachmentLogicImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceReactionsLogic> { PersistenceReactionsLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceMembersLogic> { PersistenceMembersLogicImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceUsersLogic> { PersistenceUsersLogicImpl(get(), get(), get(), get()) }
    single<PersistenceConnectionLogic> { PersistenceConnectionLogicImpl(get(), get(), get(), get()) }

    single<FileTransferLogic> { FileTransferLogicImpl(get()) }
}

internal val repositoryModule = module {
    factory<ChannelsRepository> { ChannelsRepositoryImpl() }
    factory<ProfileRepository> { ProfileRepositoryImpl() }
    factory<MessagesRepository> { MessagesRepositoryImpl() }
    factory<AttachmentsRepository> { AttachmentsRepositoryImpl() }
    factory<ReactionsRepository> { ReactionsRepositoryImpl() }
    factory<UsersRepository> { UsersRepositoryImpl() }
    factory<MessageMarkersRepository> { MessageMarkersRepositoryImpl() }
}

internal val cacheModule = module {
    single { MessagesCache() }
    single { ChannelsCache() }
    factory { AttachmentsCache() }
}

internal val viewModelModule = module {
    viewModel { params ->
        MessageListViewModel(params.get(), params.get(), params.get())
    }
    viewModel { ChannelMembersViewModel(get(), get()) }
    viewModel { ReactionsInfoViewModel() }
}


@OptIn(DelicateCoroutinesApi::class)
internal val coroutineModule = module {
    single {
        CoroutineExceptionHandler { _, throwable ->
            if (BuildConfig.DEBUG)
                SceytLog.e("Coroutine", "An exception accrued in base CoroutineExceptionHandler", throwable)
        }
    }
    single<CoroutineScope> { GlobalScope }
    single(qualifier = named(CoroutineContextType.Ui)) { providesUiContext(get()) }
    single(qualifier = named(CoroutineContextType.IO)) { providesIOContext(get()) }
    single(qualifier = named(CoroutineContextType.Computation)) { providesComputationContext(get()) }
    single(qualifier = named(CoroutineContextType.SingleThreaded)) { providesSingleThreadedContext(get()) }
}

fun providesUiContext(exceptionHandler: CoroutineExceptionHandler) =
        Dispatchers.Main + exceptionHandler

fun providesIOContext(exceptionHandler: CoroutineExceptionHandler): CoroutineContext =
        Dispatchers.IO + exceptionHandler

fun providesComputationContext(exceptionHandler: CoroutineExceptionHandler): CoroutineContext =
        Executors.newCachedThreadPool().asCoroutineDispatcher().plus(exceptionHandler)

fun providesSingleThreadedContext(exceptionHandler: CoroutineExceptionHandler): CoroutineContext =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().plus(exceptionHandler)

