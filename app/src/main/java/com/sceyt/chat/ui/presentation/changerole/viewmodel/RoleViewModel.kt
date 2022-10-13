package com.sceyt.chat.ui.presentation.changerole.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.ui.presentation.changerole.adapter.RoleItem
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoleViewModel : BaseViewModel() {
    private val _rolesLiveData = MutableLiveData<List<RoleItem>>()
    val rolesLiveData: LiveData<List<RoleItem>> = _rolesLiveData

    fun getRoles() {
        viewModelScope.launch(Dispatchers.IO) {
            ClientWrapper.getRoles { roles, status ->
                if (status == null || status.isOk) {
                    _rolesLiveData.postValue(roles.map { RoleItem(it) })
                } else
                    notifyPageStateWithResponse(SceytResponse.Error<List<Role>>(status.error))
            }
        }
    }
}