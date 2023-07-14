package com.sceyt.chat.ui.presentation.login

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.ConnectionState.Connected
import com.sceyt.chat.models.ConnectionState.Disconnected
import com.sceyt.chat.models.ConnectionState.Failed
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.SceytUiKitApp
import com.sceyt.chat.ui.data.AppSharedPreference
import com.sceyt.chat.ui.databinding.ActivityLoginBinding
import com.sceyt.chat.ui.presentation.mainactivity.MainActivity
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver
import com.sceyt.sceytchatuikit.extensions.hideSoftInput
import com.sceyt.sceytchatuikit.extensions.launchActivity
import com.sceyt.sceytchatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.uicomponents.profile.viewmodel.ProfileViewModel
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class LoginActivity : AppCompatActivity() {
    private val viewModel: ProfileViewModel by viewModels()
    private val preference by inject<AppSharedPreference>()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityLoginBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground(SceytKitConfig.isDarkMode)

        if (preference.getUserId().isNullOrBlank().not()) {
            launchActivity<MainActivity>()
            finish()
        }

        initViewModel()
        binding.initViews()
        checkState()
    }

    private fun initViewModel() {
        viewModel.pageStateLiveData.observe(this) {
            binding.loading = it is PageState.StateLoading
        }

        viewModel.editProfileLiveData.observe(this) {
            launchActivity<MainActivity>()
            finish()
        }
    }

    private fun ActivityLoginBinding.initViews() {
        userNameTextField.editText?.doAfterTextChanged { text ->
            userNameTextField.error = null
            if (text?.trim().isNullOrEmpty()) {
                userNameTextField.endIconDrawable = null
            } else
                userNameTextField.setEndIconDrawable(R.drawable.ic_check_input)

            checkState()
        }

        displayNameTextField.editText?.doAfterTextChanged { text ->
            if (text?.trim().isNullOrEmpty()) {
                displayNameTextField.endIconDrawable = null
            } else
                displayNameTextField.setEndIconDrawable(R.drawable.ic_check_input)
        }

        submitButton.setOnClickListener {
            hideSoftInput()
            loginUser(
                userNameTextField.editText?.text?.trim().toString(),
                displayNameTextField.editText?.text?.trim().toString()
            )
        }
    }

    private fun loginUser(userId: String, displayName: String) {
        binding.loading = true
        var job: Job? = null

        lifecycleScope.launch {
            preference.setUserId(userId)
            job = ConnectionEventsObserver.onChangedConnectStatusFlow.onEach {
                when (it.state) {
                    Connected -> {
                        viewModel.saveProfile(displayName, null, null, false)
                        job?.cancel()
                    }

                    Disconnected -> {
                        binding.userNameTextField.error = it.exception?.message ?: "Disconnected"
                        job?.cancel()
                    }

                    Failed -> {
                        binding.userNameTextField.error = getString(R.string.connection_failed)
                        job?.cancel()
                    }

                    else -> {}
                }
            }.launchIn(this)

            (application as SceytUiKitApp).connectChatClient()
        }
    }

    private fun checkState() {
        binding.enableConnect = binding.userNameInput.text.isNullOrBlank().not()
    }

    companion object {
        fun launch(context: Context) {
            context.launchActivity<LoginActivity>()
        }
    }
}