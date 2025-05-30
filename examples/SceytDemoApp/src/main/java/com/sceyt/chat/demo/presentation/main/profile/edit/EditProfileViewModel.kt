package com.sceyt.chat.demo.presentation.main.profile.edit

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.data.repositories.UserRepository
import com.sceyt.chat.demo.presentation.Constants.CORRECT_USERNAME_REGEX
import com.sceyt.chat.demo.presentation.common.ui.UsernameValidationEnum
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.isNotNullOrBlank
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Suppress("OPT_IN_USAGE")
class EditProfileViewModel(
        private val userRepository: UserRepository
) : BaseViewModel() {
    private val userInteractor: UserInteractor by lazy { SceytChatUIKit.chatUIFacade.userInteractor }

    private val _currentUserLiveData = MutableLiveData<SceytUser>()
    val currentUserLiveData = _currentUserLiveData.asLiveData()

    private val _editProfileLiveData = MutableLiveData<SceytUser>()
    val editProfileLiveData = _editProfileLiveData.asLiveData()

    private val _editProfileErrorLiveData = MutableLiveData<String?>()
    val editProfileErrorLiveData = _editProfileErrorLiveData.asLiveData()

    private val _usernameValidationLiveData = MutableLiveData<UsernameValidationEnum>()
    val usernameValidationLiveData = _usernameValidationLiveData.asLiveData()

    private val _usernameInput = MutableStateFlow("")
    val usernameInput: StateFlow<String> get() = _usernameInput

    private val _nextButtonEnabledLiveData = MutableLiveData<Boolean>()
    val nextButtonEnabledLiveData = _nextButtonEnabledLiveData.asLiveData()

    private var isUsernameValid: Boolean = true
    private var isFirstNameValid: Boolean = false
    private var isAvatarChanged: Boolean = false

    init {
        getCurrentUser()
        _usernameInput
            .debounce(200)
            .distinctUntilChanged()
            .onEach { username ->
                if (isValidUsername(username)) {
                    validateUsername(username)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun getCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val user = userInteractor.getCurrentUser()
            _currentUserLiveData.postValue(user ?: return@launch)
        }
    }

    fun saveProfile(
            firstName: String?,
            lastName: String?,
            username: String,
            avatarUrl: String?,
            shouldUploadAvatar: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var newUrl = avatarUrl
            if (shouldUploadAvatar && avatarUrl != null) {
                val uploadResult = userInteractor.uploadAvatar(avatarUrl)
                if (uploadResult is SceytResponse.Success) {
                    newUrl = uploadResult.data
                } else {
                    _editProfileErrorLiveData.postValue(uploadResult.message)
                    return@launch
                }
            }
            val currentUser = _currentUserLiveData.value ?: userInteractor.getCurrentUser()
            ?: return@launch
            when (val response = userInteractor.updateProfile(
                username, firstName, lastName,
                newUrl, currentUser.metadataMap
            )) {
                is SceytResponse.Success -> {
                    _editProfileLiveData.postValue(response.data ?: return@launch)
                }

                is SceytResponse.Error -> {
                    _editProfileErrorLiveData.postValue(response.message)
                }
            }
        }
    }

    fun updateUsernameInput(username: String) {
        _usernameInput.value = username
    }

    private fun validateUsername(username: String) {
        viewModelScope.launch {
            val result = userRepository.checkUsername(username)
            if (result.isSuccess) {
                _usernameValidationLiveData.postValue(UsernameValidationEnum.Valid)
            } else {
                val exception = result.exceptionOrNull()
                if (exception is SceytException && exception.code == 400) {
                    _usernameValidationLiveData.postValue(UsernameValidationEnum.AlreadyExists)
                }
            }
        }
    }

    private fun isValidUsername(username: String): Boolean {
        return when {
            username == (_currentUserLiveData.value?.username ?: "") -> {
                false
            }

            username.length !in 3..20 -> {
                _usernameValidationLiveData.postValue(UsernameValidationEnum.IncorrectSize)
                false
            }

            !username.matches(CORRECT_USERNAME_REGEX.toRegex()) -> {
                _usernameValidationLiveData.postValue(UsernameValidationEnum.InvalidCharacters)
                false
            }

            else -> true
        }
    }

    private fun setNextButtonEnabledState() {
        val enabled = isUsernameValid && isFirstNameValid
        _nextButtonEnabledLiveData.postValue(enabled)
    }

    fun setFirstNameValidState(isValid: Boolean) {
        isFirstNameValid = isValid
        setNextButtonEnabledState()
    }

    fun setUsernameValidState(isValid: Boolean) {
        isUsernameValid = isValid
        setNextButtonEnabledState()
    }

    fun setAvatarChangedState(filePath: String?) {
        isAvatarChanged = filePath.isNotNullOrBlank()
        setNextButtonEnabledState()
    }
}