package com.sceyt.chat.demo.presentation.welcome.accounts_bottomsheet

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.Constants
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectAccountsBottomSheetViewModel(
        private val sharedPreference: AppSharedPreference
) : BaseViewModel() {

    private val _accountsLiveData = MutableLiveData<List<SceytUser>>()
    val accountsLiveData: LiveData<List<SceytUser>> = _accountsLiveData

    init {
        getAccounts()
    }

    private fun getAccounts() {
        viewModelScope.launch {
            val accounts = createAccounts()
            _accountsLiveData.value = accounts
        }
    }

    private suspend fun createAccounts() = withContext(Dispatchers.Default) {
        val storedUsers = sharedPreference.getUserIdList().map { SceytUser(id = it) }
        val demoUsers = Constants.users.map { SceytUser(id = it) }.shuffled()
        return@withContext storedUsers + demoUsers
    }
}