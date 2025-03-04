package com.sceyt.chat.demo.di

import com.sceyt.chat.demo.BuildConfig
import com.sceyt.chat.demo.connection.ChatClientConnectionInterceptor
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.AppSharedPreferenceImpl
import com.sceyt.chat.demo.data.api.AuthApiService
import com.sceyt.chat.demo.data.api.UserApiService
import com.sceyt.chat.demo.data.interceptors.RetryInterceptor
import com.sceyt.chat.demo.data.repositories.ConnectionRepo
import com.sceyt.chat.demo.data.repositories.UserRepository
import com.sceyt.chat.demo.presentation.main.profile.UserProfileViewModel
import com.sceyt.chat.demo.presentation.main.profile.edit.EditProfileViewModel
import com.sceyt.chat.demo.presentation.splash.SplashViewModel
import com.sceyt.chat.demo.presentation.welcome.accounts_bottomsheet.SelectAccountsBottomSheetViewModel
import com.sceyt.chat.demo.presentation.welcome.create.CreateAccountViewModel
import com.sceyt.chat.demo.presentation.welcome.welcome.WelcomeViewModel
import com.sceyt.chatuikit.presentation.components.role.viewmodel.RoleViewModel
import com.sceyt.chatuikit.presentation.components.select_users.viewmodel.UsersViewModel
import okhttp3.OkHttpClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


const val AUTH_SERVICE = "AuthService"
const val VALIDATOR_SERVICE = "ValidatorService"

val appModules = module {
    single<AppSharedPreference> { AppSharedPreferenceImpl(get()) }
    single { ChatClientConnectionInterceptor(get(), get()) }
    single { SceytConnectionProvider(get(), get(), get()) }
}

val viewModelModules = module {
    viewModel { UsersViewModel() }
    viewModel { RoleViewModel() }
    viewModel { CreateAccountViewModel(get(), get(), get()) }
    viewModel { EditProfileViewModel(get()) }
    viewModel { SelectAccountsBottomSheetViewModel(get()) }
    viewModel { WelcomeViewModel(get(), get()) }
    viewModel { SplashViewModel(get()) }
    viewModel { UserProfileViewModel(get(), get()) }
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

    single(named(AUTH_SERVICE)) {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BuildConfig.GEN_TOKEN_BASE_URL)
            .apply { client(get()) }
            .build()
    }

    single(named(VALIDATOR_SERVICE)) {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BuildConfig.VALIDATION_API_URL)
            .client(OkHttpClient.Builder().build())
            .build()
    }

    single<AuthApiService> {
        get<Retrofit>(named(AUTH_SERVICE)).create(AuthApiService::class.java)
    }

    single<UserApiService> {
        get<Retrofit>(named(VALIDATOR_SERVICE)).create(UserApiService::class.java)
    }
}

val repositoryModule = module {
    factory { ConnectionRepo(get()) }
    single { UserRepository(get()) }
}