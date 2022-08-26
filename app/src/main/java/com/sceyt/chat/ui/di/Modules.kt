package com.sceyt.chat.ui.di

import com.sceyt.chat.ui.data.AppSharedPreference
import com.sceyt.chat.ui.data.AppSharedPreferenceImpl
import org.koin.dsl.module

val appModules = module {
    single<AppSharedPreference> { AppSharedPreferenceImpl(get()) }
}
