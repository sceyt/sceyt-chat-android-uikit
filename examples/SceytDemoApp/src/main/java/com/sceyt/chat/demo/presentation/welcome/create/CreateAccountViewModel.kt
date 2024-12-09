package com.sceyt.chat.demo.presentation.welcome.create

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.sceyt.chat.demo.connection.SceytConnectionProvider
import com.sceyt.chat.demo.data.AppSharedPreference
import com.sceyt.chat.demo.data.repositories.HttpStatusException
import com.sceyt.chat.demo.data.repositories.UserRepository
import com.sceyt.chat.demo.presentation.Constants.CORRECT_USERNAME_REGEX
import com.sceyt.chat.demo.presentation.common.ui.UsernameValidationEnum
import com.sceyt.chat.models.ConnectionState
import com.sceyt.chat.models.SceytException
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.persistence.extensions.asLiveData
import com.sceyt.chatuikit.persistence.interactor.UserInteractor
import com.sceyt.chatuikit.presentation.root.BaseViewModel
import com.sceyt.chatuikit.presentation.root.PageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume


@Suppress("OPT_IN_USAGE")
class CreateAccountViewModel(
        private val preference: AppSharedPreference,
        private val connectionProvider: SceytConnectionProvider,
        private val userRepository: UserRepository
) : BaseViewModel() {

    private val userInteractor: UserInteractor by lazy { SceytChatUIKit.chatUIFacade.userInteractor }

    private val _logInLiveData = MutableLiveData<Boolean>()
    val logInLiveData = _logInLiveData.asLiveData()

    private val _correctUsernameValidatorLiveData = MutableLiveData<UsernameValidationEnum>()
    val correctUsernameValidatorLiveData = _correctUsernameValidatorLiveData.asLiveData()

    private val _usernameInput = MutableStateFlow("")
    val usernameInput = _usernameInput.asStateFlow()

    private val _nextButtonEnabledLiveData = MutableLiveData<Boolean>()
    val nextButtonEnabledLiveData = _nextButtonEnabledLiveData.asLiveData()

    private var isUsernameValid: Boolean = false
    private var isFirstNameValid: Boolean = false

    init {
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

    fun loginUser(
            userId: String,
            firstName: String? = null,
            lastName: String? = null,
            username: String
    ) {
        notifyPageLoadingState(false)
        viewModelScope.launch {
            val result = connectUser(userId)
            if (result.isSuccess) {
                preference.setString(AppSharedPreference.PREF_USER_ID, userId)
                updateProfile(firstName = firstName, lastName = lastName, username = username)
            } else
                pageStateLiveDataInternal.value = PageState.StateError(
                    null, result.exceptionOrNull()?.message
                            ?: "Connection failed"
                )

            pageStateLiveDataInternal.value = PageState.StateLoading(false)
            _logInLiveData.value = result.isSuccess
        }
    }

    private suspend fun updateProfile(firstName: String?, lastName: String?, username: String) =
            withContext(Dispatchers.IO) {
                val currentUser = SceytChatUIKit.currentUser ?: userInteractor.getCurrentUser()
                ?: return@withContext SceytResponse.Error<SceytUser>(
                    SceytException(
                        0,
                        "User not found"
                    )
                )
                userInteractor.updateProfile(
                    username = username,
                    firstName = firstName,
                    lastName = lastName,
                    currentUser.avatarURL,
                    currentUser.metadataMap
                )
            }

    private suspend fun connectUser(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            var job: Job? = null
            suspendCancellableCoroutine { continuation ->
                job = ConnectionEventManager.onChangedConnectStatusFlow.onEach {
                    when (it.state) {
                        ConnectionState.Connected -> {
                            continuation.resume(Result.success(true))
                            job?.cancel()
                        }

                        ConnectionState.Disconnected, ConnectionState.Failed -> {
                            continuation.resume(
                                Result.failure(
                                    Exception(
                                        it.exception?.message
                                                ?: "Connection failed"
                                    )
                                )
                            )
                            job?.cancel()
                        }

                        else -> {}
                    }
                }.launchIn(this)
                connectionProvider.connectChatClient(userId)
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
                _correctUsernameValidatorLiveData.postValue(UsernameValidationEnum.Valid)
            } else {
                val exception = result.exceptionOrNull()
                if (exception is HttpStatusException && exception.statusCode == 400) {
                    _correctUsernameValidatorLiveData.postValue(UsernameValidationEnum.AlreadyExists)
                }
            }
        }
    }

    private fun isValidUsername(username: String): Boolean {
        return when {
            username.length !in 3..20 -> {
                _correctUsernameValidatorLiveData.postValue(UsernameValidationEnum.IncorrectSize)
                false
            }

            !username.matches(CORRECT_USERNAME_REGEX.toRegex()) -> {
                _correctUsernameValidatorLiveData.postValue(UsernameValidationEnum.InvalidCharacters)
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

    fun setUserNameValidState(isValid: Boolean) {
        isUsernameValid = isValid
        setNextButtonEnabledState()
    }
}
