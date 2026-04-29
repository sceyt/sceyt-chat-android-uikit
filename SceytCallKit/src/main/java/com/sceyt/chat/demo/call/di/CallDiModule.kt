package com.sceyt.chat.demo.call.di

import com.sceyt.chat.demo.call.manager.CallManager
import com.sceyt.chat.demo.call.manager.CallManagerImpl
import com.sceyt.chat.demo.call.ui.CallMembersViewModel
import com.sceyt.chat.demo.call.ui.CallViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

fun callModule(onChatConnectNeeded: () -> Unit = {}) = module {
    single<CallManager> {
        CallManagerImpl(
            context = get(),
            onChatConnectNeeded = onChatConnectNeeded
        )
    }
    viewModelOf(::CallViewModel)
    viewModelOf(::CallMembersViewModel)
}
