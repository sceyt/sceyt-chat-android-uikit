package com.sceyt.chatuikit.persistence.di

import android.content.Context
import androidx.room.Room
import com.sceyt.chatuikit.BuildConfig
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.PersistenceMiddleWareImpl
import com.sceyt.chatuikit.persistence.SceytDatabase
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferService
import com.sceyt.chatuikit.persistence.filetransfer.FileTransferServiceImpl
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageMarkerInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.PersistenceMessageMarkerLogic
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMembersLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceUsersLogic
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceConnectionLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceMembersLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceMessageMarkerLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceReactionsLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceUsersLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.attachmentlogic.AttachmentsCache
import com.sceyt.chatuikit.persistence.logicimpl.attachmentlogic.PersistenceAttachmentLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.PersistenceChannelsLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.messageslogic.MessageLoadRangeUpdater
import com.sceyt.chatuikit.persistence.logicimpl.messageslogic.MessagesCache
import com.sceyt.chatuikit.persistence.logicimpl.messageslogic.PersistenceMessagesLogicImpl
import com.sceyt.chatuikit.services.SceytSyncManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

const val SCEYT_CHAT_UI_KIT_DATABASE_NAME = "sceyt_ui_kit_database"

internal val appModules = module {
    single { SceytSyncManager(get(), get(), get()) }
    single<FileTransferService> { FileTransferServiceImpl(get(), get()) }
    single<MessageLoadRangeUpdater> { MessageLoadRangeUpdater(get()) }
}

internal fun databaseModule(enableDatabase: Boolean) = module {

    fun provideDatabase(context: Context): SceytDatabase {
        val builder = if (enableDatabase)
            Room.databaseBuilder(context, SceytDatabase::class.java, SCEYT_CHAT_UI_KIT_DATABASE_NAME)
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
    single { get<SceytDatabase>().markerDao() }
}

internal val interactorModule = module {
    single { PersistenceMiddleWareImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<ChannelInteractor> { get<PersistenceMiddleWareImpl>() }
    single<MessageInteractor> { get<PersistenceMiddleWareImpl>() }
    single<AttachmentInteractor> { get<PersistenceMiddleWareImpl>() }
    single<MessageMarkerInteractor> { get<PersistenceMiddleWareImpl>() }
    single<MessageReactionInteractor> { get<PersistenceMiddleWareImpl>() }
    single<ChannelMemberInteractor> { get<PersistenceMiddleWareImpl>() }
    single<UserInteractor> { get<PersistenceMiddleWareImpl>() }
    single<SceytChatUIFacade> { SceytChatUIFacade(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}

internal val logicModule = module {
    single<PersistenceChannelsLogic> { PersistenceChannelsLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceMessagesLogic> { PersistenceMessagesLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceAttachmentLogic> { PersistenceAttachmentLogicImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceReactionsLogic> { PersistenceReactionsLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceMembersLogic> { PersistenceMembersLogicImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceUsersLogic> { PersistenceUsersLogicImpl(get(), get(), get(), get()) }
    single<PersistenceMessageMarkerLogic> { PersistenceMessageMarkerLogicImpl(get(), get(), get()) }
    single<PersistenceConnectionLogic> { PersistenceConnectionLogicImpl(get(), get(), get(), get()) }
    single<FileTransferLogic> { FileTransferLogicImpl(get(), get()) }
}

internal val cacheModule = module {
    single { MessagesCache() }
    single { ChannelsCache() }
    factory { AttachmentsCache() }
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

