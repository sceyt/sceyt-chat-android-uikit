package com.sceyt.chat.ui.di

import com.sceyt.chat.ui.data.AppSharedPreference
import com.sceyt.chat.ui.data.AppSharedPreferenceImpl
import com.sceyt.chat.ui.presentation.addmembers.viewmodel.UsersViewModel
import com.sceyt.chat.ui.presentation.changerole.viewmodel.RoleViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModules = module {
    single<AppSharedPreference> { AppSharedPreferenceImpl(get()) }
}

val viewModelModules = module {
    viewModel { UsersViewModel() }
    viewModel { RoleViewModel() }
}
