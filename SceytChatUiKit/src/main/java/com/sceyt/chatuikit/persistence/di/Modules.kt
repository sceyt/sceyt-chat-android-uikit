package com.sceyt.chatuikit.persistence.di

import android.content.Context
import androidx.room.Room
import com.sceyt.chatuikit.BuildConfig
import com.sceyt.chatuikit.SceytChatUIFacade
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.notifications.managers.RealtimeNotificationManager
import com.sceyt.chatuikit.notifications.managers.RealtimeNotificationManagerImpl
import com.sceyt.chatuikit.persistence.PersistenceMiddleWareImpl
import com.sceyt.chatuikit.persistence.database.DatabaseMigrations
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.cleaner.DatabaseCleaner
import com.sceyt.chatuikit.persistence.database.cleaner.DatabaseCleanerImpl
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferService
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferServiceImpl
import com.sceyt.chatuikit.persistence.interactor.AttachmentInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelInteractor
import com.sceyt.chatuikit.persistence.interactor.ChannelMemberInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageMarkerInteractor
import com.sceyt.chatuikit.persistence.interactor.MessageReactionInteractor
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.persistence.logic.FileTransferLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceAttachmentLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceConnectionLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMembersLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessageMarkerLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceReactionsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceUsersLogic
import com.sceyt.chatuikit.persistence.logicimpl.FileTransferLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceConnectionLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceMembersLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceMessageMarkerLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceReactionsLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.PersistenceUsersLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.attachment.AttachmentsCache
import com.sceyt.chatuikit.persistence.logicimpl.attachment.PersistenceAttachmentLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.logicimpl.channel.PersistenceChannelsLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.message.MessageLoadRangeUpdater
import com.sceyt.chatuikit.persistence.logicimpl.message.MessagesCache
import com.sceyt.chatuikit.persistence.logicimpl.message.PersistenceMessagesLogicImpl
import com.sceyt.chatuikit.persistence.logicimpl.usecases.ShouldShowNotificationUseCase
import com.sceyt.chatuikit.push.service.PushService
import com.sceyt.chatuikit.push.service.PushServiceImpl
import com.sceyt.chatuikit.services.SceytSyncManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

const val SCEYT_CHAT_UI_KIT_DATABASE_NAME = "sceyt_ui_kit_database"

internal val appModules = module {
    single { SceytSyncManager(get(), get()) }
    single<FileTransferService> { FileTransferServiceImpl(get(), get()) }
    single<MessageLoadRangeUpdater> { MessageLoadRangeUpdater(get()) }
    single<PushService> { PushServiceImpl(get(), get(), get(), get()) }
    single<RealtimeNotificationManager> { RealtimeNotificationManagerImpl(get(), get()) }
}

internal fun databaseModule(enableDatabase: Boolean) = module {

    fun provideDatabase(context: Context): SceytDatabase {
        val builder = if (enableDatabase)
            Room.databaseBuilder(context, SceytDatabase::class.java, SCEYT_CHAT_UI_KIT_DATABASE_NAME)
        else
            Room.inMemoryDatabaseBuilder(context, SceytDatabase::class.java)

        return builder
            .fallbackToDestructiveMigration()
            .addMigrations(DatabaseMigrations.Migration_15_16, DatabaseMigrations.Migration_16_17)
            .allowMainThreadQueries()
            .build()
    }

    single { provideDatabase(get()) }
    single<DatabaseCleaner> { DatabaseCleanerImpl(get()) }
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
    single { get<SceytDatabase>().autoDeleteMessageDao() }
}

internal val interactorModule = module {
    single { PersistenceMiddleWareImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<ChannelInteractor> { get<PersistenceMiddleWareImpl>() }
    single<MessageInteractor> { get<PersistenceMiddleWareImpl>() }
    single<AttachmentInteractor> { get<PersistenceMiddleWareImpl>() }
    single<MessageMarkerInteractor> { get<PersistenceMiddleWareImpl>() }
    single<MessageReactionInteractor> { get<PersistenceMiddleWareImpl>() }
    single<ChannelMemberInteractor> { get<PersistenceMiddleWareImpl>() }
    single<UserInteractor> { get<PersistenceMiddleWareImpl>() }
    single<SceytChatUIFacade> { SceytChatUIFacade(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}

internal val logicModule = module {
    single<PersistenceChannelsLogic> { PersistenceChannelsLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceMessagesLogic> { PersistenceMessagesLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceAttachmentLogic> { PersistenceAttachmentLogicImpl(get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceReactionsLogic> { PersistenceReactionsLogicImpl(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceMembersLogic> { PersistenceMembersLogicImpl(get(), get(), get(), get(), get(), get(), get()) }
    single<PersistenceUsersLogic> { PersistenceUsersLogicImpl(get(), get(), get(), get()) }
    single<PersistenceMessageMarkerLogic> { PersistenceMessageMarkerLogicImpl(get(), get(), get()) }
    single<PersistenceConnectionLogic> { PersistenceConnectionLogicImpl(get(), get(), get(), get()) }
    single<FileTransferLogic> { FileTransferLogicImpl(get(), get()) }
}

internal val useCaseModule = module {
    factory { ShouldShowNotificationUseCase(get()) }
}

internal val cacheModule = module {
    single { MessagesCache() }
    single { ChannelsCache() }
    factory { AttachmentsCache() }
}

internal val coroutineModule = module {
    single {
        CoroutineExceptionHandler { _, throwable ->
            if (BuildConfig.DEBUG)
                SceytLog.e("Coroutine", "An exception accrued in base CoroutineExceptionHandler", throwable)
        }
    }
    single<CoroutineScope> { CoroutineScope(SupervisorJob()) }
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

