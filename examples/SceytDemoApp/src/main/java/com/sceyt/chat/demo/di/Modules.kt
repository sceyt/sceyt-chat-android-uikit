package com.sceyt.chat.demo.di

import com.sceyt.chat.demo.BuildConfig
import com.sceyt.chat.demo.connection.ChatClientConnectionInterceptor
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.AppSharedPreferenceImpl
import com.sceyt.chat.demo.data.api.AuthApiService
import com.sceyt.chat.demo.data.interceptors.RetryInterceptor
import com.sceyt.chat.demo.data.repositories.ConnectionRepo
import com.sceyt.chat.demo.presentation.login.SelectAccountsBottomSheetViewModel
import com.sceyt.chat.demo.presentation.login.create.CreateProfileViewModel
import com.sceyt.chat.demo.presentation.login.welcome.WelcomeViewModel
import com.sceyt.chat.demo.presentation.main.profile.edit.EditProfileViewModel
import com.sceyt.chat.demo.presentation.splash.SplashViewModel
import com.sceyt.chatuikit.presentation.components.role.viewmodel.RoleViewModel
import com.sceyt.chatuikit.presentation.components.select_users.viewmodel.UsersViewModel
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
    viewModel { CreateProfileViewModel(get(), get()) }
    viewModel { EditProfileViewModel() }
    viewModel { SelectAccountsBottomSheetViewModel() }
    viewModel { WelcomeViewModel(get(), get()) }
    viewModel { SplashViewModel(get()) }
}

val apiModule = module {
    single {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor(3, 2000))
            .build()
    }

    single<Retrofit> {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BuildConfig.GEN_TOKEN_BASE_URL)
            .apply { client(get()) }
            .build()
    }

    factory<AuthApiService> { get<Retrofit>().create(AuthApiService::class.java) }
}

val repositoryModule = module {
    factory { ConnectionRepo(get()) }
}