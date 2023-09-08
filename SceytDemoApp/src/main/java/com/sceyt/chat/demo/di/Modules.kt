package com.sceyt.chat.demo.di

import com.sceyt.chat.demo.data.interceptors.RetryInterceptor
import com.sceyt.chat.demo.connection.ChatClientConnectionInterceptor
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.AppSharedPreferenceImpl
import com.sceyt.chat.demo.data.api.AuthApiService
import com.sceyt.chat.demo.data.repositories.ConnectionRepo
import com.sceyt.chat.demo.presentation.addmembers.viewmodel.UsersViewModel
import com.sceyt.chat.demo.presentation.changerole.viewmodel.RoleViewModel
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

val appModules = module {
    single<AppSharedPreference> { AppSharedPreferenceImpl(get()) }
    single { ChatClientConnectionInterceptor(get(), get()) }
    single { SceytConnectionProvider(get(), get(), get()) }
}

val viewModelModules = module {
    viewModel { UsersViewModel() }
    viewModel { RoleViewModel() }
}

val apiModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor(3,2000))
            .build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://tlnig20qy7.execute-api.us-east-2.amazonaws.com/")
            .apply { client(get()) }
            .build()
    }

    factory<AuthApiService> { get<Retrofit>().create(AuthApiService::class.java) }
}

val repositoryModule = module {
    factory { ConnectionRepo(get()) }
}