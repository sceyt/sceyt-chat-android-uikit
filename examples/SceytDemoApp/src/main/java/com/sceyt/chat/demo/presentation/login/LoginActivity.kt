package com.sceyt.chat.demo.presentation.login

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import com.sceyt.chat.demo.R
import com.sceyt.chat.demo.databinding.ActivityLoginBinding
import com.sceyt.chat.demo.presentation.mainactivity.MainActivity
import com.sceyt.chatuikit.extensions.customToastSnackBar
import com.sceyt.chatuikit.extensions.hideSoftInput
import com.sceyt.chatuikit.extensions.launchActivity
import com.sceyt.chatuikit.extensions.statusBarIconsColorWithBackground
import com.sceyt.chatuikit.presentation.root.PageState
import org.koin.androidx.viewmodel.ext.android.viewModel

class LoginActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModel()
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(ActivityLoginBinding.inflate(layoutInflater)
            .also { binding = it }
            .root)

        statusBarIconsColorWithBackground()

        if (viewModel.isLoggedIn()) {
            launchActivity<MainActivity>()
            finish()
        }

        initViewModel()
        binding.initViews()
        checkState()
    }

    private fun initViewModel() {
        viewModel.pageStateLiveData.observe(this) { pageState ->
            when (pageState) {
                is PageState.StateLoading -> binding.loading = pageState.isLoading
                is PageState.StateError -> customToastSnackBar(pageState.errorMessage
                        ?: return@observe)

                else -> return@observe
            }
        }

        viewModel.logInLiveData.observe(this) {
            if (it) {
                launchActivity<MainActivity>()
                finish()
            }
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
            viewModel.loginUser(
                userNameTextField.editText?.text?.trim().toString(),
                displayNameTextField.editText?.text?.trim().toString()
            )
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