package com.sceyt.chat.ui.presentation.login

import android.content.Context
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.SceytUiKitApp
import com.sceyt.chat.ui.data.UserSharedPreference
import com.sceyt.chat.ui.databinding.ActivityLoginBinding
import com.sceyt.chat.ui.extensions.hideSoftInput
import com.sceyt.chat.ui.extensions.isNightTheme
import com.sceyt.chat.ui.extensions.launchActivity
import com.sceyt.chat.ui.extensions.statusBarIconsColorWithBackground
import com.sceyt.chat.ui.presentation.login.viewmodel.LoginViewModel
import com.sceyt.chat.ui.presentation.mainactivity.MainActivity
import com.sceyt.chat.ui.presentation.root.PageState

class LoginActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        statusBarIconsColorWithBackground(isNightTheme())

        if (UserSharedPreference.getUsername(this).isNullOrBlank().not()) {
            launchActivity<MainActivity>()
            finish()
        }


        setContentView(ActivityLoginBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        initViewModel()
        binding.initViews()
        checkState()
    }

    private fun initViewModel() {
        viewModel.pageStateLiveData.observe(this) {
            binding.loading = it is PageState.StateLoading
        }

        viewModel.editNameLiveData.observe(this) {
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

        (application as SceytUiKitApp).connectWithoutToken(userId)
            .observe(this) { success ->
                if (success == true) {
                    viewModel.updateDisplayName(displayName)
                } else {
                    binding.userNameTextField.error = getString(R.string.connection_failed)
                    binding.loading = false
                }
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