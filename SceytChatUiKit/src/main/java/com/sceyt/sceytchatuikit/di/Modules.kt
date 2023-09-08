package com.sceyt.sceytchatuikit.di

import android.content.Context
import androidx.room.Room
import com.sceyt.sceytchatuikit.BuildConfig
import com.sceyt.sceytchatuikit.SceytSyncManager
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.SceytSharedPreferenceImpl
import com.sceyt.sceytchatuikit.data.repositories.AttachmentsRepository
import com.sceyt.sceytchatuikit.data.repositories.AttachmentsRepositoryImpl
import com.sceyt.sceytchatuikit.data.repositories.ChannelsRepository
import com.sceyt.sceytchatuikit.data.repositories.ChannelsRepositoryImpl
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepository
import com.sceyt.sceytchatuikit.data.repositories.MessagesRepositoryImpl
import com.sceyt.sceytchatuikit.data.repositories.ProfileRepository
import com.sceyt.sceytchatuikit.data.repositories.ProfileRepositoryImpl
import com.sceyt.sceytchatuikit.data.repositories.ReactionsRepository
import com.sceyt.sceytchatuikit.data.repositories.ReactionsRepositoryImpl
import com.sceyt.sceytchatuikit.data.repositories.UsersRepository
import com.sceyt.sceytchatuikit.data.repositories.UsersRepositoryImpl
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.persistence.PersistenceAttachmentsMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceChanelMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMembersMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMessagesMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceMiddleWareImpl
import com.sceyt.sceytchatuikit.persistence.PersistenceReactionsMiddleWare
import com.sceyt.sceytchatuikit.persistence.PersistenceUsersMiddleWare
import com.sceyt.sceytchatuikit.persistence.SceytDatabase
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferServiceImpl
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogic
import com.sceyt.sceytchatuikit.persistence.logics.attachmentlogic.PersistenceAttachmentLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogic
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.connectionlogic.PersistenceConnectionLogic
import com.sceyt.sceytchatuikit.persistence.logics.connectionlogic.PersistenceConnectionLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.filetransferlogic.FileTransferLogic
import com.sceyt.sceytchatuikit.persistence.logics.filetransferlogic.FileTransferLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.memberslogic.PersistenceMembersLogic
import com.sceyt.sceytchatuikit.persistence.logics.memberslogic.PersistenceMembersLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.AttachmentsCache
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.MessagesCache
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.reactionslogic.PersistenceReactionsLogic
import com.sceyt.sceytchatuikit.persistence.logics.reactionslogic.PersistenceReactionsLogicImpl
import com.sceyt.sceytchatuikit.persistence.logics.userslogic.PersistenceUsersLogic
import com.sceyt.sceytchatuikit.persistence.logics.userslogic.PersistenceUsersLogicImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.viewmodels.ReactionsInfoViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.viewmodels.MessageListViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.viewmodel.ChannelAttachmentsViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.members.viewmodel.ChannelMembersViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.viewmodel.ConversationInfoViewModel
import com.sceyt.sceytchatuikit.presentation.uicomponents.creategroup.viewmodel.CreateChatViewModel
import com.sceyt.sceytchatuikit.services.networkmonitor.ConnectionStateService
import com.sceyt.sceytchatuikit.services.networkmonitor.ConnectionStateServiceImpl
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
    single<ConnectionStateService> { ConnectionStateServiceImpl(get()) }
    single { SceytSyncManager(get(), get(), get()) }
    single<FileTransferService> { FileTransferServiceImpl(get(), get()) }
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

    single { PersistenceMiddleWareImpl(get(), get(), get(), get(), get(), get(), get()) }
    factory<PersistenceChanelMiddleWare> { get<PersistenceMiddleWareImpl>() }
    factory<PersistenceMessagesMiddleWare> { get<PersistenceMiddleWareImpl>() }
    factory<PersistenceAttachmentsMiddleWare> { get<PersistenceMiddleWareImpl>() }
    factory<PersistenceReactionsMiddleWare> { get<PersistenceMiddleWareImpl>() }
    factory<PersistenceMembersMiddleWare> { get<PersistenceMiddleWareImpl>() }
    factory<PersistenceUsersMiddleWare> { get<PersistenceMiddleWareImpl>() }

    factory<PersistenceChannelsLogic> { PersistenceChannelsLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory<PersistenceMessagesLogic> { PersistenceMessagesLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory<PersistenceAttachmentLogic> { PersistenceAttachmentLogicImpl(get(), get(), get(), get(), get(), get(), get()) }
    factory<PersistenceReactionsLogic> { PersistenceReactionsLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory<PersistenceMembersLogic> { PersistenceMembersLogicImpl(get(), get(), get(), get(), get(), get()) }
    factory<PersistenceUsersLogic> { PersistenceUsersLogicImpl(get(), get(), get(), get()) }
    factory<PersistenceConnectionLogic> { PersistenceConnectionLogicImpl(get(), get(), get()) }

    single<FileTransferLogic> { FileTransferLogicImpl(get()) }
}

internal val repositoryModule = module {
    factory<ChannelsRepository> { ChannelsRepositoryImpl() }
    factory<ProfileRepository> { ProfileRepositoryImpl() }
    factory<MessagesRepository> { MessagesRepositoryImpl() }
    factory<AttachmentsRepository> { AttachmentsRepositoryImpl() }
    factory<ReactionsRepository> { ReactionsRepositoryImpl() }
    factory<UsersRepository> { UsersRepositoryImpl() }
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
    viewModel { ChannelAttachmentsViewModel() }
    viewModel { ChannelMembersViewModel(get(), get()) }
    viewModel { CreateChatViewModel() }
    viewModel { ConversationInfoViewModel() }
    viewModel { ChannelsViewModel() }
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
    single(qualifier = named(CoroutineContextType.Disk)) { providesDiskContext(get()) }
    single(qualifier = named(CoroutineContextType.Network)) { providesNetworkContext(get()) }
    single(qualifier = named(CoroutineContextType.Computation)) { providesComputationContext(get()) }
    single(qualifier = named(CoroutineContextType.Database)) { providesDatabaseContext(get()) }
}

private fun providesUiContext(exceptionHandler: CoroutineExceptionHandler) =
        Dispatchers.Main + exceptionHandler

private fun providesDiskContext(exceptionHandler: CoroutineExceptionHandler) =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().plus(exceptionHandler)

fun providesNetworkContext(exceptionHandler: CoroutineExceptionHandler): CoroutineContext =
        Dispatchers.IO + exceptionHandler

fun providesComputationContext(exceptionHandler: CoroutineExceptionHandler): CoroutineContext =
        Executors.newCachedThreadPool().asCoroutineDispatcher().plus(exceptionHandler)

fun providesDatabaseContext(exceptionHandler: CoroutineExceptionHandler): CoroutineContext =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher().plus(exceptionHandler)

