package com.sceyt.chat.ui.di

import android.app.Application
import androidx.room.Room
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.ui.data.*
import com.sceyt.chat.ui.persistence.SceytDatabase
import com.sceyt.chat.ui.presentation.mainactivity.profile.viewmodel.ProfileViewModel
import com.sceyt.chat.ui.presentation.uicomponents.channels.viewmodels.ChannelsViewModel
import org.koin.android.ext.koin.androidApplication
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
    single { provideDatabase(androidApplication()) }
    single { get<SceytDatabase>().channelDao() }
    single { get<SceytDatabase>().userDao() }
    single { get<SceytDatabase>().messageDao() }
}

val repositoryModule = module {
    factory<ChannelsRepository> { ChannelsRepositoryImpl() }
    factory<MessagesRepository> { (conversationId: Long, channel: Channel, rep: Boolean) ->
        MessagesRepositoryImpl(conversationId, channel, rep)
    }
}

val viewModels = module {
    viewModel { ChannelsViewModel(get()) }
    viewModel { ProfileViewModel(get(), get()) }
}
